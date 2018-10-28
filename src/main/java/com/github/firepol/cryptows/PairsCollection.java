package com.github.firepol.cryptows;

import org.knowm.xchange.currency.CurrencyPair;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PairsCollection {

    public List<CurrencyPair> pairs = new ArrayList<>();

    public PairsCollection(File file) {
        try {
            Scanner input = new Scanner(file);

            while (input.hasNextLine()) {
                String line = input.nextLine();
                if (line.startsWith("#")) continue;
                List<String> symbol = Arrays.asList(line.split(","));
                if (symbol.size() != 2) {
                    throw new InvalidParameterException(
                            String.format("%s: %s is an invalid pair, valid pair example: BTC,USD",
                                    file.getName(),
                                    line));
                }
                pairs.add(new CurrencyPair(symbol.get(0), symbol.get(1)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}

