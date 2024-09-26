package com.faken.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogBean {
    private String projectId;
    private String logLevel;
    private String logTime;
    private String logContent;
}
