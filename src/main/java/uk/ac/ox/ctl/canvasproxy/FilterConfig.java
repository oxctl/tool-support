package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ViaFilter> viaFilter() {
        FilterRegistrationBean<ViaFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new ViaFilter());
        registrationBean.addUrlPatterns("/api/*"); // All the proxy URLs
        registrationBean.setOrder(1); 

        return registrationBean;
    }
}