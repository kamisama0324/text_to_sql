package com.kami.springai.datasource.filter;

import com.kami.springai.datasource.context.DataSourceContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 数据源上下文过滤器
 * 用于从请求头中获取数据源ID并设置到上下文环境中
 */
@Slf4j
@Component
public class DataSourceContextFilter implements Filter {

    /**
     * 数据源ID的请求头名称
     */
    private static final String DATA_SOURCE_HEADER = "X-DataSource-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String dataSourceId = httpRequest.getHeader(DATA_SOURCE_HEADER);

        try {
            if (dataSourceId != null && !dataSourceId.isEmpty()) {
                log.debug("设置数据源ID: {}", dataSourceId);
                DataSourceContextHolder.setDataSourceId(dataSourceId);
            } else {
                log.debug("未指定数据源ID，使用默认数据源");
                DataSourceContextHolder.clearDataSourceId();
            }
            
            // 继续执行过滤器链
            chain.doFilter(request, response);
        } finally {
            // 清除数据源上下文，避免线程泄漏
            DataSourceContextHolder.clearDataSourceId();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("数据源上下文过滤器初始化");
    }

    @Override
    public void destroy() {
        log.info("数据源上下文过滤器销毁");
    }
}