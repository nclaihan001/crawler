package nc.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import nc.crawler.command.Crawler;
import nc.crawler.model.ScanTarget;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

@Slf4j
@SpringBootApplication
public class CrawlerRunner {
    public static void main(String[] args) throws InterruptedException, IOException {
        ApplicationContext context = SpringApplication.run(CrawlerRunner.class);
        MongoTemplate mongoTemplate = context.getBean(MongoTemplate.class);
        @Cleanup
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        @Cleanup
        Browser browser = playwright.chromium().launch(options);
        while(!Thread.interrupted()){
            Query query = new Query();
            query.addCriteria(Criteria.where(ScanTarget.Fields.executeTime).lt(LocalDateTime.now()));
            ScanTarget scanTarget = mongoTemplate.findOne(query,ScanTarget.class);
            if(Objects.nonNull(scanTarget)){
                for (Crawler value : context.getBeansOfType(Crawler.class).values()) {
                    if(value.getSource().equals(scanTarget.getSource())){
                        Crawler.Context context1 = new Crawler.Context(scanTarget, browser);
                        long time = System.currentTimeMillis();
                        log.info("执行爬虫[{}] -> type:[{}], 开始",scanTarget.getSource(),scanTarget.getType());
                        boolean check = false;
                        Update update = new Update();
                        try{
                            check=value.execute(context1);
                            update.set(ScanTarget.Fields.updateTime,scanTarget.getSuccess()+1);
                        }catch (Exception e){
                            log.error("执行爬虫["+scanTarget.getSource()+"] -> type:["+scanTarget.getType()+"], 失败",e);
                            update.set(ScanTarget.Fields.updateTime,scanTarget.getFail()+1);
                        }
                        if (!check || context1.isClean()) {
                            mongoTemplate.remove(scanTarget);
                            log.info("执行爬虫[{}] -> type:[{}], 结束: {}ms, 下一次执行时间: 永不",scanTarget.getSource(),scanTarget.getType(),System.currentTimeMillis()-time);
                        }else{
                            Date executeTime = DateUtils.addSeconds(new Date(),Long.valueOf(scanTarget.getInterval()).intValue());
                            scanTarget.setExecuteTime(executeTime);
                            query = new Query(Criteria.where(ScanTarget.Fields.id).is(scanTarget.getId()));
                            update.set(ScanTarget.Fields.executeTime,executeTime);
                            update.set(ScanTarget.Fields.updateTime,LocalDateTime.now());
                            mongoTemplate.updateFirst(query,update,ScanTarget.class);
                            log.info("执行爬虫[{}] -> type:[{}], 结束: {}ms, 下一次执行时间: {}",scanTarget.getSource(),scanTarget.getType(),System.currentTimeMillis()-time,
                                    DateFormatUtils.format(executeTime,"yyyy-MM-dd HH:mm:ss.SSS"));
                        }
                        break;
                    }
                }
            }else{
                log.warn("没有找到等待执行的目标");
                Thread.sleep(1000);
            }
        }
    }
}
