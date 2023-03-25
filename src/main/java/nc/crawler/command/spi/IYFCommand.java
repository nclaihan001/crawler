package nc.crawler.command.spi;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import lombok.extern.slf4j.Slf4j;
import nc.crawler.command.Crawler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
public class IYFCommand extends Crawler {

    public IYFCommand(MongoTemplate mongoTemplate) {
        super("iyf", mongoTemplate);
        registerTarget("https://www.iyf.tv/list/movie?page=1","IYF电影首页","index",10);
    }


    @Override
    protected Optional<Consumer<Page>> getExecute(String type) {
        if("index".equals(type)){
            return Optional.of(this::acceptIndex);
        }
        return Optional.empty();
    }

    private void acceptIndex(Page page){
    }
}
