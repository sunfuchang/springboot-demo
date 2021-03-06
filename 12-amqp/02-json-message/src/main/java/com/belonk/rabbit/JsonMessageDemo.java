package com.belonk.rabbit;

import com.belonk.pojo.Bar;
import com.belonk.pojo.Foo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

import static com.belonk.config.JsonMessageConfig.*;

/**
 * Created by sun on 2019/3/20.
 *
 * @author sunfuchang03@126.com
 * @version 1.0
 * @since 1.0
 */
@Component
public class JsonMessageDemo {
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Static fields/constants/initializer
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private static Logger log = LoggerFactory.getLogger(JsonMessageDemo.class);

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Instance fields
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitTemplate jsonRabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Constructors
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */



    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Public Methods
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public void runDemo() throws Exception {
        String json = "{\"foo\" : \"value\" }";
        Message jsonMsg = MessageBuilder.withBody(json.getBytes(Charset.forName("utf-8")))
                .andProperties(MessagePropertiesBuilder.newInstance().setContentType("application/json").build())
                .build();

        //~ Send to inferred queue which listened by method of listenForAFoo and listenForABar using @RabbitListener

        this.rabbitTemplate.send(INFERRED_FOO_QUEUE, jsonMsg);
        this.rabbitTemplate.send(INFERRED_BAR_QUEUE, jsonMsg);

        //~ Send and receive using custom jsonRabbitTemplate

        json = "{\"foo\" : \"convertAndReceive\"}";
        jsonMsg = MessageBuilder.withBody(json.getBytes(Charset.forName("utf-8")))
                .andProperties(MessagePropertiesBuilder.newInstance().setContentType("application/json").build())
                .build();
        jsonMsg.getMessageProperties().setHeader(DefaultClassMapper.DEFAULT_CLASSID_FIELD_NAME, "foo");
        this.jsonRabbitTemplate.send(RECEIVE_AND_CONVERT_QUEUE, jsonMsg);
        // receive返回map
        Foo foo = (Foo) this.jsonRabbitTemplate.receiveAndConvert(RECEIVE_AND_CONVERT_QUEUE, 10_000);
        System.out.println("convertAndReceive : Expected a Foo, got a " + foo);
        // 如果Jackson2JsonMessageConverter不设置ClassMapper，直接使用objectMapper转换对象，底层也是将json转为对象（convertBytesToObject方法）
        this.jsonRabbitTemplate.send(RECEIVE_AND_CONVERT_QUEUE, jsonMsg);
        Bar bar = objectMapper.readValue(objectMapper.writeValueAsString(this.jsonRabbitTemplate.receiveAndConvert(RECEIVE_AND_CONVERT_QUEUE, 10_000)), Bar.class);
        System.out.println("convertAndReceive : Expected a Bar, got a " + bar);

        //~ global exception handler

        // To mock some bad json message, when sending a ListenerExecutionFailedException will be thrown.
        this.rabbitTemplate.convertAndSend(INFERRED_FOO_QUEUE, new Foo("error json foo"),
                message -> new Message("bad json".getBytes(), message.getMessageProperties()));
    }

    @RabbitListener(queues = INFERRED_FOO_QUEUE)
    public void listenForAFoo(Foo foo) {
        System.out.println("listenForAFoo: Expected a Foo, got a " + foo);
    }

    @RabbitListener(queues = INFERRED_BAR_QUEUE)
    public void listenForABar(Bar bar) {
        System.out.println("listenForABar : Expected a Bar, got a " + bar);
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Protected Methods
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */



    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Property accessors
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */



    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * Inner classes
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

}