package com.faken.util;

import com.alibaba.fastjson2.JSON;
import com.faken.bean.ExceptionInfoBean;
import com.faken.bean.LogBean;
import com.faken.bean.RequestInfoBean;
import com.faken.properties.ServerProperties;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 借助Okhttp进行访问记录, 操作日志, 异常日志的上传操作
 */
public class DataSender {

    private final String serverIP;
    private final String serverPort;
    private final String projectId;

    private final OkHttpClient client;

    private final ConcurrentLinkedQueue<RequestInfoBean> requestQueue;
    private final ConcurrentLinkedQueue<ExceptionInfoBean> exceptionQueue;
    private final ConcurrentLinkedQueue<LogBean> logQueue;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledFutureOfRequestSendingTask;
    private ScheduledFuture<?> scheduledFutureOfExceptionSendingTask;
    private ScheduledFuture<?> scheduledFutureOfLogSendingTask;

    private final Logger logger = LoggerFactory.getLogger(DataSender.class);

    private final boolean openAccessLog;
    private final boolean openExceptionLog;
    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;

    private boolean TaskWorkingNormally = false;

    /**
     * 指定以下信息, 初始化<em>DataSender<em/>
     * 默认开启自定义日志传输
     *
     * @param serverProperties 服务器配置信息
     * @param openAccessLog 是否开启访问日志发送
     * @param openExceptionLog 是否开启异常记录发送
     * @param initialDelay 定时任务启动延迟时间
     * @param period 定时发送时间
     * @param unit 时间单位
     */
    public DataSender(@NotNull ServerProperties serverProperties,
                      boolean openAccessLog,
                      boolean openExceptionLog,
                      long initialDelay,
                      long period,
                      TimeUnit unit) {
        this.serverIP   = serverProperties.getServerIp();
        this.serverPort = serverProperties.getServerPort();
        this.projectId  = serverProperties.getProjectId();

        this.client     = new OkHttpClient();

        this.requestQueue   = new ConcurrentLinkedQueue<>();
        this.exceptionQueue = new ConcurrentLinkedQueue<>();
        this.logQueue       = new ConcurrentLinkedQueue<>();

        this.openAccessLog    = openAccessLog;
        this.openExceptionLog = openExceptionLog;
        this.initialDelay     = initialDelay;
        this.period           = period;
        this.unit             = unit;

        StartSendingTask(openAccessLog, openExceptionLog, initialDelay, period, unit);
        executor.scheduleAtFixedRate(
                this::monitorSendingTask,
                initialDelay + 2,
                period,
                TimeUnit.SECONDS
        );
        logger.info("start monitoring status of sending tasks");
    }

    private void StartSendingTask(boolean openAccessLog, boolean openExceptionLog, long initialDelay, long period, TimeUnit unit) {
        // 尝试连接指定服务器, 发送一次get请求进行握手注册
        if (initConnection()) {
            // 设定定时任务, 间隔多少秒之后进行数据的上传
            if (openAccessLog) {
                scheduledFutureOfRequestSendingTask = executor.scheduleAtFixedRate(
                        this::sendRequestInfoToServer,
                        initialDelay,
                        period,
                        unit
                );
            }
            if (openExceptionLog) {
                scheduledFutureOfExceptionSendingTask = executor.scheduleAtFixedRate(
                        this::sendExceptionToServer,
                        initialDelay,
                        period,
                        unit
                );
            }
            // 默认开启自定义日志的定时发送任务
            scheduledFutureOfLogSendingTask = executor.scheduleAtFixedRate(
                    this::sendCustomLogToServer,
                    initialDelay,
                    period,
                    TimeUnit.SECONDS
            );
            TaskWorkingNormally = true;
        }
    }

    private boolean initConnection() {
        logger.info("init connection {}", serverIP + ":" + serverPort);
        Request request = new Request.Builder()
                .url("http://" + serverIP + ":" + serverPort + "/sdk/backend/connect?projectId=" + projectId)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                String res = response.body().string();
                if (res.contains("CONNECT_SUCCESS")) {
                    logger.info("Successfully connect to the manage platform---{}:{}", serverIP, serverPort);
                    return true;
                } else if (res.contains("INVALID_CONNECTION")) {
                    logger.info("Invalid connection attempt, please check if your project is released in the manage platform");
                    return false;
                } else {
                    logger.error("Failed to connect to the manage platform, unknown error: {}", res);
                    return false;
                }
            } else {
                logger.error("Failed to connect to manage platform, please check your network connection or try again. IP: {}", serverIP + ":" + serverPort);
                return false;
            }
        }catch (Exception e) {
            logger.error("Failed to connect to manage platform, please inform the administrator of your platform to check if the platform is running well. IP: {}", serverIP + ":" + serverPort);
        }
        return false;
    }

    protected void addRequestInfoToQueue(String requestIp, String requestURL, String requestMethod, String controllerMappingName, String apiMappingName, LocalDateTime requestTime) throws Exception {
        RequestInfoBean requestInfoBean = new RequestInfoBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText(requestIp),
                ClientEncryptUtils.encryptText(requestURL),
                ClientEncryptUtils.encryptText(requestMethod),
                ClientEncryptUtils.encryptText(controllerMappingName),
                ClientEncryptUtils.encryptText(apiMappingName),
                ClientEncryptUtils.encryptText(String.valueOf(requestTime))
        );

        requestQueue.offer(requestInfoBean);
    }

    public void addExceptionInfoToQueue(@NotNull Exception e) throws Exception {
        ExceptionInfoBean exceptionInfoBean = new ExceptionInfoBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText(Arrays.toString(e.getStackTrace())),
                ClientEncryptUtils.encryptText(String.valueOf(LocalDateTime.now()))
        );
        exceptionQueue.offer(exceptionInfoBean);
    }

    private void sendRequestInfoToServer() {
        ConcurrentLinkedQueue<RequestInfoBean> tempQueue = new ConcurrentLinkedQueue<>(requestQueue);
        requestQueue.clear();

        List<RequestInfoBean> list = new ArrayList<>();
        RequestInfoBean requestInfoBean;
        while ((requestInfoBean = tempQueue.poll()) != null) {
            list.add(requestInfoBean);
        }

        String jsonString = JSON.toJSONString(list);

        RequestBody requestBody = RequestBody.create(jsonString, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://" + serverIP + ":" + serverPort + "/sdk/backend/accessLog")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                logger.info("Successfully sent access logs on a scheduled time");
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("Failed to send access logs on a scheduled time: {}", e.getMessage());
                scheduledFutureOfRequestSendingTask.cancel(true);
            }
        });
    }

    public void sendExceptionToServer() {
        ConcurrentLinkedQueue<ExceptionInfoBean> tempQueue = new ConcurrentLinkedQueue<>(exceptionQueue);
        exceptionQueue.clear();
        List<ExceptionInfoBean> list = new ArrayList<>();
        ExceptionInfoBean exceptionInfoBean;
        while ((exceptionInfoBean = tempQueue.poll()) != null) {
            list.add(exceptionInfoBean);
        }
        String jsonString = JSON.toJSONString(list);
        RequestBody requestBody = RequestBody.create(jsonString, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://" + serverIP + ":" + serverPort + "/sdk/backend/exceptionLog")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                logger.info("Successfully sent exception logs on a scheduled time");
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("Failed to send exception logs on a scheduled time: {}", e.getMessage());
                scheduledFutureOfExceptionSendingTask.cancel(true);
            }
        });
    }

    public void sendCustomLogToServer() {
        ConcurrentLinkedQueue<LogBean> tempQueue = new ConcurrentLinkedQueue<>(logQueue);
        logQueue.clear();
        List<LogBean> list = new ArrayList<>();
        LogBean logBean;
        while ((logBean = tempQueue.poll()) != null) {
            list.add(logBean);
        }
        String jsonString = JSON.toJSONString(list);
        RequestBody requestBody = RequestBody.create(jsonString, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://" + serverIP + ":" + serverPort + "/sdk/backend/log")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                logger.info("Successfully sent custom logs on a scheduled time");
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("Failed to send custom logs to the server: {}", e.getMessage());
                scheduledFutureOfLogSendingTask.cancel(true);
            }
        });
    }

    public void logError(String error) throws Exception {
        logger.error(error);
        logQueue.offer(new LogBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText("ERROR"),
                ClientEncryptUtils.encryptText(String.valueOf(LocalDateTime.now())),
                ClientEncryptUtils.encryptText(error)
        ));
    }

    public void logInfo(String info) throws Exception {
        logger.info(info);
        logQueue.offer(new LogBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText("INFO"),
                ClientEncryptUtils.encryptText(String.valueOf(LocalDateTime.now())),
                ClientEncryptUtils.encryptText(info)
        ));
    }

    public void logWarning(String warning) throws Exception {
        logger.warn(warning);
        logQueue.offer(new LogBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText("WARN"),
                ClientEncryptUtils.encryptText(String.valueOf(LocalDateTime.now())),
                ClientEncryptUtils.encryptText(warning)
        ));
    }

    public void logDebug(String debug) throws Exception {
        logger.debug(debug);
        logQueue.offer(new LogBean(
                ClientEncryptUtils.encryptText(this.projectId),
                ClientEncryptUtils.encryptText("DEBUG"),
                ClientEncryptUtils.encryptText(String.valueOf(LocalDateTime.now())),
                ClientEncryptUtils.encryptText(debug)
        ));
    }

    public void monitorSendingTask() {
        // 检查是否被取消了
        if ( !TaskWorkingNormally ||
                (Objects.nonNull(scheduledFutureOfExceptionSendingTask) && scheduledFutureOfExceptionSendingTask.isCancelled()) ||
                (Objects.nonNull(scheduledFutureOfRequestSendingTask) && scheduledFutureOfRequestSendingTask.isCancelled()) ||
                (Objects.nonNull(scheduledFutureOfLogSendingTask) && scheduledFutureOfLogSendingTask.isCancelled())) {
            if (TaskWorkingNormally) {
                logger.error("Data sending task is running, but received errors from the server, stopping task and reconnecting to the server");
                TaskWorkingNormally = false;
            }
            StartSendingTask(openAccessLog, openExceptionLog, initialDelay, period, unit);
        }
    }
 }
