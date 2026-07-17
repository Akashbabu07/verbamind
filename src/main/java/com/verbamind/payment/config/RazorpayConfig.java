package com.verbamind.payment.config;

import com.razorpay.RazorpayClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayConfig {

    @Bean
    public RazorpayClient razorpayClient(RazorpayProperties props) throws Exception {
        return new RazorpayClient(props.getKeyId(), props.getKeySecret());
    }
}