package nc.crawler.command;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nc.crawler.model.ScanTarget;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 爬虫生命周期
 */
@Slf4j
public abstract class Crawler{
    @Getter
    private final String source;
    private final MongoTemplate mongoTemplate;
    private final AtomicReference<Page> atomicReference = new AtomicReference<>();

    /**
     * 爬虫初始化
     * @param source 爬虫名称
     */
    public Crawler(String source, MongoTemplate mongoTemplate) {
        this.source = source;
        this.mongoTemplate = mongoTemplate;
        log.info("爬虫[{}]初始化完毕",source);
    }

    /**
     * 注册扫描目标
     * @param url 目标地址
     * @param desc 目标说明->例如: 排行榜, 详情页面
     * @param type 类型用于判断执行流程
     * @param interval 执行的间隔, 主要控制下一次的执行时间
     */
    protected void registerTarget(String url, String desc, String type, long interval){
        String id = DigestUtils.md5DigestAsHex((source+"/"+url).getBytes(StandardCharsets.UTF_8));
        Update update = new Update();
        update.setOnInsert(ScanTarget.Fields.source,source);
        update.setOnInsert(ScanTarget.Fields.url,url);
        update.setOnInsert(ScanTarget.Fields.success,0);
        update.setOnInsert(ScanTarget.Fields.fail,0);
        update.setOnInsert(ScanTarget.Fields.executeTime, LocalDateTime.now());
        update.setOnInsert(ScanTarget.Fields.updateTime, LocalDateTime.now());
        update.set(ScanTarget.Fields.desc,desc);
        update.set(ScanTarget.Fields.type,type);
        update.set(ScanTarget.Fields.interval,interval);
        mongoTemplate.upsert(Query.query(Criteria.where(ScanTarget.Fields.id).is(id)),update,ScanTarget.class);
    }
    public void before(Context context){

    }
    public void complete(Context context){

    }
    protected abstract Optional<Consumer<Page>> getExecute(String type);
    /**
     * 执行逻辑
     */
    public final boolean execute(Context context){
        ScanTarget scanTarget = context.scanTarget;
        Page page = newPage(context.browser);
        try{
            page.navigate(context.scanTarget.getUrl());
            Optional<Consumer<Page>> optional = getExecute(context.scanTarget.getType());
            if(optional.isEmpty()){
                return false;
            }
            before(context);
            optional.get().accept(page);
            return true;
        }finally {
            try{
                complete(context);
            }catch (Exception e){
                log.error("执行爬虫["+scanTarget.getSource()+"] -> type:["+scanTarget.getType()+"], 失败",e);
            }
            close(page);
        }
    }

    /**
     * 创建访问页面
     * @return
     */
    public Page newPage(Browser browser){
        return atomicReference.updateAndGet(page -> {
            if(Objects.isNull(page)){
                page = browser.newContext().newPage();
                return page;
            }
            return page;
        });
    }
    public void close(Page page){

    }
    @Data
    @RequiredArgsConstructor
    public static class Context{
        private final ScanTarget scanTarget;
        private boolean clean = false;
        private final Browser browser;
    }
}
