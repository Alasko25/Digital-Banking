package com.example.benoit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Testlombon {
    private String name;

    public static void main(String[] args) {
        Testlombon obj = new Testlombon();
        obj.setName("Test Ok");
        System.out.println(obj.getName());
    }
}
