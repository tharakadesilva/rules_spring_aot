package com.example;

import org.springframework.shell.command.annotation.Command;

@Command
public class HelloCommand {

    @Command(command = "hello")
    public void sayHello() {
        System.out.println("Hello from Spring Shell!");
    }
}
