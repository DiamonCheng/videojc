package com.dc.videojc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/***
 * IndexController...
 * @author Diamon.Cheng
 * @date 2021/6/18
 */
@RestController
public class IndexController {
    @RequestMapping({"", "index"})
    public String index() {
        return "Service vediojc running!";
    }
}
