package com.simpleton.socket.controller;

import com.sun.org.apache.xpath.internal.operations.Mod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Author : Harry
 * Description :
 * Date : 2020-05-15 11:21
 */
@RestController
public class DemoController {

    @GetMapping("index")
    public ResponseEntity<String> index(){
        return ResponseEntity.ok("请求成功");
    }

    @GetMapping(value = "/page")
    public ModelAndView page(String toUserId, String fromUserId){

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("websocket");
        modelAndView.addObject("toUserId", toUserId);
        modelAndView.addObject("fromUserId", fromUserId);
        return modelAndView;
    }

    /**
     * 群发所有
     */
    @RequestMapping("/push/all")
    public String pushToWeb(String message) {

        WebSocketServer.sendAll(message);
        return "发送成功 !";
    }
}
