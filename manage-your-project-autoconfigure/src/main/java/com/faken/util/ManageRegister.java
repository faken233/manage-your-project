package com.faken.util;


import com.faken.annotation.Api;
import com.faken.annotation.Model;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ManageRegister {
    /**
     * Controller的映射路径和用户指定控制器名的绑定关系
     */
    private final Map<String, String> ModelPathMap  = new HashMap<>();

    /**
     * 接口的映射路径和用户指定接口名的映射关系
     */
    private final Map<String, Map<String, String>> ApiPathMap = new HashMap<>();

    /**
     * 这是一个构造器附带注册的方法, 在配置类中实现{@code ManageConfigurer}后, 使用这个构造器方法
     * 生成一个实例, 交给ioc容器管理.
     * <p>使用者将所有控制类的字节码对象传入构造方法, 构造方法创建实例后将返回实例.<p/>
     *
     *
     * @param controllerClasses 传入的控制器字节码文件
     */
    public ManageRegister(@NotNull Class<?>...controllerClasses) {
        for (Class<?> clazz : controllerClasses) {
            Model modelAnno = clazz.getDeclaredAnnotation(Model.class);
            RequestMapping requestMappingAnno = clazz.getDeclaredAnnotation(RequestMapping.class);

            if (modelAnno != null && requestMappingAnno != null) {
                // 绑定Controller路径和控制器指定名的对应关系
                ModelPathMap.put(requestMappingAnno.value()[0], modelAnno.value());

                // 添加控制器路径到ApiPathMap中
                ApiPathMap.put(modelAnno.value(), new HashMap<>());
            } else {
                continue;
            }

            // 获取所有接口, 读取接口的路径以及指定的接口名
            Method[] declaredMethods = clazz.getDeclaredMethods();
            Map<String, String> apiMap = ApiPathMap.get(modelAnno.value());
            for (Method method : declaredMethods) {

                Api apiAnno = method.getDeclaredAnnotation(Api.class);
                GetMapping getMappingAnno = method.getDeclaredAnnotation(GetMapping.class);
                PostMapping postMappingAnno = method.getDeclaredAnnotation(PostMapping.class);
                DeleteMapping deleteMappingAnno = method.getDeclaredAnnotation(DeleteMapping.class);
                PutMapping putMappingAnno = method.getDeclaredAnnotation(PutMapping.class);

                // 如果同时存在@Api还有四个mapping注解之一, 则绑定一个关系
                if (apiAnno != null && (getMappingAnno != null || postMappingAnno != null || deleteMappingAnno != null || putMappingAnno != null)) {
                    String apiPath;
                    if (getMappingAnno != null) {
                        apiPath = getMappingAnno.value()[0];
                    } else if (postMappingAnno != null) {
                        apiPath = postMappingAnno.value()[0];
                    } else if (deleteMappingAnno != null) {
                        apiPath = deleteMappingAnno.value()[0];
                    } else {
                        apiPath = putMappingAnno.value()[0];
                    }
                    apiMap.put(apiPath, apiAnno.value());
                }
            }
        }
    }

}
