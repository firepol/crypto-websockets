package com.github.firepol.cryptows;

import org.knowm.xchange.currency.CurrencyPair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PairsCollection {

    public List<CurrencyPair> pairs = new ArrayList<>();

    public PairsCollection(String fileName) {
        try {
            File file = new File(fileName);
            Scanner input = new Scanner(file);

            while (input.hasNextLine()) {
                String line = input.nextLine();
                if (line.startsWith("#")) continue;
                List<String> symbol = Arrays.asList(line.split(","));
                pairs.add(new CurrencyPair(symbol.get(0), symbol.get(1)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}

