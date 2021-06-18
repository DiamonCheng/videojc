package com.dc.videojc.controller;

import com.dc.videojc.base.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <p>Descriptions...
 *
 * @author Diamon.Cheng
 * @date 2018/5/15.
 */
@Controller
@RequestMapping("/error")
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName()
                                                          + ".ERROR";
    private static final String JAVAX_EX_KEY = "javax.servlet.error.exception";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorController.class);
    
    @RequestMapping(produces = "text/html")
    @ResponseBody
    public Object error(HttpServletRequest request,
                        HttpServletResponse response) {
        Throwable e = getError(request);
        if (e != null) {
            LOGGER.error("ERROR " + response.getStatus(), e);
        }
        return "ERROR " + response.getStatus();
    }
    
    @ResponseBody
    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Object errorAjax(HttpServletRequest request,
                            HttpServletResponse response) {
        AjaxResult res = new AjaxResult();
        res.setSuccess(false);
        String message = null;
        Throwable ex = getError(request);
        if (ex != null) {
            LOGGER.error("拦截器捕获到异常", ex);
            if (ex instanceof MaxUploadSizeExceededException) {
                message = "上传文件过大（超过1M）";
            }
            //TODO translate exception by config
            if (!StringUtils.hasLength(message)) {
                message = ex.getClass().getSimpleName() + ":" + ex.getLocalizedMessage();
            }
            res.setMessage(message);
            res.setData(printExString(ex));
        } else {
            res.setMessage(response.getStatus() + "");
        }
        return res;
    }
    
    @RequestMapping("/404")
    @ResponseBody
    public Object error404(HttpServletRequest request) {
        return "error/404";
    }
    
    @RequestMapping("/403")
    @ResponseBody
    public Object error403(HttpServletRequest request) {
        return "error/403";
    }
    
    @RequestMapping(value = {"/500"}, produces = "text/html")
    @ResponseBody
    public String error500(HttpServletRequest request) {
        Throwable ex = getError(request);
        String exStr = "";
        if (ex != null) {
            exStr = printExString(getError(request));
        }
        return "Internal Server Error 500\n" + exStr;
    }
    
    public Throwable getError(HttpServletRequest request) {
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        Throwable exception = getAttribute(requestAttributes, ERROR_ATTRIBUTE);
        if (exception == null) {
            exception = getAttribute(requestAttributes, JAVAX_EX_KEY);
        }
        return exception;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
        return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }
    
    public static String printExString(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
