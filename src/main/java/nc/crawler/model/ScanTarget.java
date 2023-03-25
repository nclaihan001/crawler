package nc.crawler.model;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@FieldNameConstants
@Document(collection="scan_task_list")
public class ScanTarget {
    @Id
    private String id;
    @Indexed(name = "scan_target_index")
    private Date executeTime;
    @Indexed(name = "scan_target_source")
    private String source;
    private String url;
    private String desc;
    private String type;
    private long interval;
    private long success;
    private long fail;
    private Date updateTime;
}
