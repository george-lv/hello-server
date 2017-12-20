package com.greentown.smscenter.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.ImportResource;

import zipkin.server.EnableZipkinServer;


@EnableDiscoveryClient
@SpringBootApplication
@EnableHystrix
@EnableZipkinServer
@ImportResource({"classpath*:applicationContext.xml"})
public class SmsCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmsCenterApplication.class, args);
	}
}


