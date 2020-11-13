package com.example.dyx;

public class InterfaceTest {
    private message mMessage=new ZjjActivity();


    public interface message{
        void sendMessage(String s);
    }

    public  void send(){
        mMessage.sendMessage("zjj");
    }

}
