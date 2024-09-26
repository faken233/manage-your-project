package com.faken.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionInfoBean {
    private String projectId;
    private String exception;
    private String errorTime;
}
