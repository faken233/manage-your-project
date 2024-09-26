package com.faken.util;


import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestManageProcessor {
    /**
     * Controller的映射路径和用户指定控制器名的绑定关系
     */
    private final Map<String, String> modelPathMap;

    /**
     * 接口的映射路径和用户指定接口名的映射关系
     */
    private final Map<String, Map<String, String>> apiPathMap;

    private final DataSender dataSender;

    public RequestManageProcessor(ManageRegister manageRegister, DataSender dataSender) {
        this.modelPathMap = manageRegister.getModelPathMap();
        this.apiPathMap = manageRegister.getApiPathMap();
        this.dataSender = dataSender;


    }

    public void processRequest(HttpServletRequest request) throws Exception {
        // 获取请求路径, 进行映射
        String requestPath = request.getRequestURI();

        // 分割路径
        List<String> pathParts = Arrays.asList(requestPath.split("/"));

        // 过滤掉空字符串
        pathParts = pathParts.stream()
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());

        // 提取第一级路径
        String controllerPath = pathParts.isEmpty() ? "" : "/" + pathParts.get(0);

        // 提取剩余路径
        String apiPath = pathParts.size() > 1
                ? "/" + String.join("/", pathParts.subList(1, pathParts.size()))
                : "";
        if ("/error".equals(controllerPath)) {
            return;
        }

        // 获取请求方法, 不处理预检方法
        String method = request.getMethod();
        if ("OPTIONS".equals(method)) {
            return;
        }

        // 拿到IP地址
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        String controllerMappingName = modelPathMap.get(controllerPath);
        controllerMappingName = controllerMappingName == null ? "" : controllerMappingName;

        if (controllerMappingName.isEmpty()){
            return;
        }

        String apiMappingName = apiPathMap.get(controllerMappingName).get(apiPath);
        apiMappingName = apiMappingName == null ? "" : apiMappingName;

        // 没有读取到控制器到接口的映射关系，不发送访问日志
        if (apiMappingName.isEmpty()) {
            return;
        }

        dataSender.addRequestInfoToQueue(ip, requestPath,method, controllerMappingName, apiMappingName, LocalDateTime.now());
    }
}
