package org.example;

import java.util.Scanner;

public class Input {
    public static int nextInt(){
        Scanner scn = new Scanner(System.in);
        return scn.nextInt();
    }
    public static int nextInt(String text){
        System.out.print(text);
        return nextInt();
    }

    public static String next(){
        Scanner scn = new Scanner(System.in);
        return scn.next();
    }
    public static String next(String text){
        System.out.print(text);
        return next();
    }

    public static String nextLine(String text){
        System.out.print(text);
        return new Scanner(System.in).nextLine();
    }
}
