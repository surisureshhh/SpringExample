package com.sample.common;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	ApplicationContext context = new ClassPathXmlApplicationContext("Spring-Module.xml");
    	 
        HelloWorld obj = (HelloWorld) context.getBean("helloBean");
        obj.printHello();
        obj.generateDSL();
        
    }
}
