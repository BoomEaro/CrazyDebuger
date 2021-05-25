package ru.boomearo.crazydebuger.objects.essmoney;

import java.math.BigDecimal;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.utils.NumberUtil;

public class EssentialsMoney implements IMoney {

    private final Essentials ess;

    public EssentialsMoney(Essentials ess) {
        this.ess = ess;
    }

    @Override
    public String getMoney(String user) {
        BigDecimal bd = this.ess.getUser(user).getMoney();
        return NumberUtil.displayCurrency(bd, this.ess);
    }

}
