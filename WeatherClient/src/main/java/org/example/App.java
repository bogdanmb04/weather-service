package org.example;

import java.io.IOException;
import network.Client;

public class App 
{
    public static void main( String[] args )
    {
        try {
            new Client().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
