package ru.boomearo.crazydebuger.objects.essmoney;

public class EmptyMoney implements IMoney {

    @Override
    public String getMoney(String name) {
        return null;
    }

}
