package com.usthe.tom.sureness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usthe.sureness.mgt.SurenessSecurityManager;
import com.usthe.sureness.processor.exception.*;
import com.usthe.tom.pojo.dto.Message;
import com.usthe.tom.support.log.LogExeManager;
import com.usthe.tom.support.log.LogTaskFactory;
import com.usthe.sureness.subject.SubjectSum;
import com.usthe.sureness.util.SurenessContextHolder;
import com.usthe.tom.sureness.processor.RefreshExpiredTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * sureness filter class example, filter all http request
 * @author tomsun28
 * @date 23:22 2020-03-02
 */
@Order(1)
@WebFilter(filterName = "SurenessFilterExample", urlPatterns = "/*", asyncSupported = true)
public class SurenessFilterExample implements Filter {

    /** logger **/
    private static final Logger logger = LoggerFactory.getLogger(SurenessFilterExample.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        try {
            SubjectSum subject = SurenessSecurityManager.getInstance().checkIn(servletRequest);
            // You can consider using SurenessContextHolder to bind subject in threadLocal
            // if bind, please remove it when end
            if (subject != null) {
                SurenessContextHolder.bindSubject(subject);
                LogExeManager.getInstance().executeLogTask(LogTaskFactory.operatorLog((String) subject.getPrincipal(), (String) subject.getTargetResource(), true, "允许访问"));
            }
        } catch (ProcessorNotFoundException | UnknownAccountException | UnsupportedSubjectException e4) {
            logger.debug("this request is illegal");
            responseWrite(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST).body("bad request, can not Auth"), servletResponse);
            return;
        } catch (DisabledAccountException | ExcessiveAttemptsException e2 ) {
            logger.debug("the account is disabled");
            responseWrite(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).body(e2.getMessage()), servletResponse);
            return;
        } catch (IncorrectCredentialsException e3) {
            logger.debug("this account credential is incorrect");
            responseWrite(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).body(e3.getMessage()), servletResponse);
            return;
        } catch (ExpiredCredentialsException e3) {
            logger.debug("this account credential is expired");
            Message message = Message.builder().errorMsg("expired").build();
            responseWrite(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).body(message), servletResponse);
            return;
        } catch (RefreshExpiredTokenException e3) {
            logger.debug("this account credential is expired");
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("token", e3.getMessage());
            Message message = Message.builder().data(dataMap).errorMsg("refreshToken").build();
            responseWrite(ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED).body(message), servletResponse);
            return;
        } catch (UnauthorizedException e5) {
            logger.debug("this account can not access this resource");
            responseWrite(ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(e5.getMessage()), servletResponse);
            return;
        } catch (RuntimeException e) {
            logger.error("other exception happen: ", e);
            responseWrite(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(),
                    servletResponse);
            return;
        }
        try {
            // if ok, doFilter and add subject in request
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            SurenessContextHolder.clear();
        }
    }

    /**
     * write response json data
     * @param content content
     * @param response response
     */
    private void responseWrite(ResponseEntity<?> content, ServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");
        ((HttpServletResponse)response).setStatus(content.getStatusCodeValue());
        content.getHeaders().forEach((key, value) ->
                ((HttpServletResponse) response).addHeader(key, value.get(0)));
        try (PrintWriter printWriter = response.getWriter()) {
            if (content.getBody() != null) {
                if (content.getBody() instanceof String) {
                    printWriter.write(content.getBody().toString());
                } else {
                    ObjectMapper objectMapper = new ObjectMapper();
                    printWriter.write(objectMapper.writeValueAsString(content.getBody()));
                }
            } else {
                printWriter.flush();
            }
        } catch (IOException e) {
            logger.error("responseWrite response error: ", e);
        }
    }
}
