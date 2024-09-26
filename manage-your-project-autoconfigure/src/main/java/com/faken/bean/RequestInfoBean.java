package com.faken.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInfoBean {
    private String projectId;
    private String requestIp;
    private String requestUrl;
    private String requestMethod;
    private String requestController;
    private String requestApi;
    private String requestTime;
}
