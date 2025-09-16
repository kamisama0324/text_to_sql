package com.kami.springai.mcp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 主页控制器
 * 
 * 处理静态资源访问和主页重定向，确保用户访问根路径时能正确加载前端界面。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Controller
public class HomeController {

    /**
     * 主页路由
     * 
     * 当用户访问根路径 "/" 时，直接转发到静态资源主页。
     * 使用forward而不是redirect避免重定向循环。
     * 
     * @return 转发到index.html
     */
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    /**
     * Demo页面路由
     * 
     * 提供友好的demo访问路径，方便用户记忆和访问。
     * 
     * @return 转发到index.html
     */
    @GetMapping("/demo")
    public String demo() {
        return "forward:/index.html";
    }

    /**
     * Text2SQL页面路由
     * 
     * 提供直观的text2sql访问路径。
     * 
     * @return 转发到index.html
     */
    @GetMapping("/text2sql")
    public String text2sql() {
        return "forward:/index.html";
    }
}