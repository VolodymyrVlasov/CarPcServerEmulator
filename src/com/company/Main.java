package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Main {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private static volatile boolean state = true;
    private static final String UNSUBSCRIBE = "@a0",
            SUBSCRIBE = "@a1",
            PARENT_DIRECTORY = "..",
            DIAG = "diag",
            CONFIG = "config",
            NEW_LINE = "\n",
            DIR = "?";
    private static final int MAX_VOLTAGE_ON_ANALOG_INPUT = 143;
    private static final int MAX_TEMP_ON_TEMP_INPUT = 1000;
    private static final int MAX_CELL_VOLTAGE = 4250;
    private static final int REDUCTION = 8;
    private static int batteryCapacity = 0,
            batteryVoltage = 0,
            batteryCurrent = 0,
            speed = 0,
            range = 0,
            passedDistance = 0,
            totalDistance = 1200,
            rpm = 0;
    private static volatile boolean flag = true,
            flagSendAllParams = true,
            flagRead = true,
            flagCapacity = true,
            flagSpeed = true,
            flagVoltage = true,
            flagCurrent = true,
            flagParentDirectory = false,
            flagDiag = false,
            flagDiagEntered = false,
            flagConfig = false,
            flagConfigEntered = false,
            flagDIR = false,
            flagNewLine = false;

    public static void main(String[] args) {
        try (ServerSocket ss = new ServerSocket(8080)) {

            while (true) {
                System.out.println(Thread.currentThread().getName() + ": Wait client");
                Socket s = ss.accept();
                System.out.println(Thread.currentThread().getName() + ": Client connected");
                new Thread(() -> {

                    try {
                        Scanner scanner = new Scanner(s.getInputStream());
                        PrintWriter writer = new PrintWriter(s.getOutputStream(), true);
                        //input stream thread
                        new Thread(() -> {
                            boolean flag = true;
                            while (flag) {
                                try {
                                    System.out.println("Wait input");
                                    Thread.sleep(1);
                                    String request = scanner.nextLine();
                                    System.out.println(request);
                                    switch (request) {
                                        case UNSUBSCRIBE:
                                            if (flagSendAllParams) {
                                                flagSendAllParams = false;
                                                System.out.println(">> " + request + " -> UNSUBSCRIBE\n");
                                            }
                                            break;
                                        case SUBSCRIBE:
                                            if (!flagSendAllParams) {
                                                flagSendAllParams = true;
                                                System.out.println(">> " + request + " -> SUBSCRIBE\n");
                                            }
                                            break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("86");
                                    flag = false;
                                    state = false;
                                    Thread.currentThread().interrupt();
                                    try {
                                        s.close();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                        System.out.println("94");
                                    }
                                }
                            }
                        }).start();

                        //output stream thread
                        new Thread(() -> {
                            try {
                                while (true) {
                                    if (flagSendAllParams) {
                                        String temp = getValue();
                                        writer.println(temp);
                                        System.out.println(temp);
                                       Thread.sleep(new Random().nextInt(50)+50);
                                    }
                                }
                            } catch (Exception e) {
                                try {
                                    e.printStackTrace();
                                    System.out.println("112");
                                    s.close();
                                    state = false;
                                    Thread.currentThread().interrupt();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    System.out.println("118");
                                }
                            }
                        }).start();

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("124");
                    }

                }).start();
                System.out.println("state " + state);
                if (!state) Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("131");
        }
        System.out.println("End MAIN");
    }

    public static String getValue() {
        char[] chars = new char[]{'V', 'l', 'n', 'm', 'w', 'z', 'q', 'i', 't', 'v', 'c', 'f', 'R', 'F', 'r', 's', 'E'}; //, 'V', 'l', 'n', 'm', 'w', 'z', 'q', 'i', 't'
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        String result = "&E0";
        int randomCell = random.nextInt(95);
        int randomVoltageCell = random.nextInt(1200) + 3000;
        int randomTempCell = random.nextInt(600) - 100;
        char symbolOfParameter = chars[random.nextInt(chars.length - 1)];

        if (symbolOfParameter == 'm' || symbolOfParameter == 'n') {
            stringBuilder.append("&");
            stringBuilder.append(symbolOfParameter);
            stringBuilder.append(randomCell);
            stringBuilder.append(":");
            stringBuilder.append(randomVoltageCell);
            result = stringBuilder.toString();

            result = "&" + symbolOfParameter + "" + randomCell + ":" + randomVoltageCell;
        }
        if (symbolOfParameter == 'z' || symbolOfParameter == 'w') {
            result = "&" + symbolOfParameter + "" + randomCell + ":" + randomTempCell;
        }
        if (symbolOfParameter == 'i') {
            result = "&" + symbolOfParameter +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT) +
                    ":" + random.nextInt(MAX_VOLTAGE_ON_ANALOG_INPUT);
        }
        if (symbolOfParameter == 't') {
            result = "&" + symbolOfParameter +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT) +
                    ":" + random.nextInt(MAX_TEMP_ON_TEMP_INPUT);
        }
        if (symbolOfParameter == 'q') {
            result = "&" + symbolOfParameter + randomCell +
                    ":" + random.nextInt(MAX_CELL_VOLTAGE) +
                    ":" + (random.nextInt(800) - 200) +
                    ":" + (random.nextInt(800) - 200) +
                    ":" + "0" +
                    ":" + "1";

        }
        if (symbolOfParameter == 'l') {
            if (batteryCapacity < 1000 && flagCapacity) {
                batteryCapacity += random.nextInt(20) + 10;
                result = "&" + symbolOfParameter + "" + batteryCapacity;
            }
            if (batteryCapacity > 0 && !flagCapacity) {
                batteryCapacity -= random.nextInt(20) + 10;
                if (batteryCapacity >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryCapacity;
                }
            }
            if (batteryCapacity <= 0) {
                flagCapacity = true;
            } else if (batteryCapacity >= 1000) {
                flagCapacity = false;
            }
        }
        if (symbolOfParameter == 'R') {
            range = map(batteryCapacity, 0, 1000, 0, 3000);
            result = "&" + symbolOfParameter + "" + range;
        }
        if (symbolOfParameter == 'f') {
            passedDistance = 3000 - range;
            totalDistance += 10;
            result = "&" + symbolOfParameter + "" + passedDistance;
        }
        if (symbolOfParameter == 'F') {
            result = "&" + symbolOfParameter + "" + totalDistance;
        }
        if (symbolOfParameter == 'r') {
            rpm = speed * REDUCTION;
            result = "&" + symbolOfParameter + "" + rpm;
        }
        if (symbolOfParameter == 'v') {
            if (batteryVoltage < 4050 && flagVoltage) {
                batteryVoltage += random.nextInt(40) + 10;
                result = "&" + symbolOfParameter + "" + batteryVoltage;
            }
            if (batteryVoltage > 2500 && !flagVoltage) {
                batteryVoltage -= random.nextInt(40) + 10;
                if (batteryVoltage >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryVoltage;
                }
            }
            if (batteryVoltage <= 2500) {
                flagVoltage = true;
            } else if (batteryVoltage >= 4050) {
                flagVoltage = false;
            }
        }
        if (symbolOfParameter == 'c') {
            if (batteryCurrent < 2500 && flagCurrent) {
                batteryCurrent += random.nextInt(40) + 10;
                result = "&" + symbolOfParameter + "" + batteryCurrent;
            }
            if (batteryCurrent > 0 && !flagCurrent) {
                batteryCurrent -= random.nextInt(40) + 10;
                if (batteryCurrent >= 0) {
                    result = "&" + symbolOfParameter + "" + batteryCurrent;
                }
            }
            if (batteryCurrent <= 0) {
                flagCurrent = true;
            } else if (batteryCurrent >= 2500) {
                flagCurrent = false;
            }
        }
        if (symbolOfParameter == 'V') {
            if (speed < 180 && flagSpeed) {
                speed += random.nextInt(9) + 1;
                result = "&" + symbolOfParameter + "" + speed;
            }
            if (speed > 0 && !flagSpeed) {
                speed -= random.nextInt(9) + 1;
                if (speed >= 0) {
                    result = "&" + symbolOfParameter + "" + speed;
                }
            }
            if (speed <= 0) {
                flagSpeed = true;
            } else if (speed >= 180) {
                flagSpeed = false;
            }
        }
        if (symbolOfParameter == 's') {
            result = "&" + symbolOfParameter + "" + random.nextInt(13);
        }
        if (symbolOfParameter == 'E') {
            String[] arr = new String[]{"0", "0x1", "0x2", "0x4", "0x8", "0x10", "0x20", "0x40", "0x100", "0x200", "0x400"};
            result = "&" + symbolOfParameter + arr[random.nextInt(arr.length - 1)];
        }
        return result;
    }

    public static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
}
