package com.witchersbestiary.economy;

/** Checked arithmetic shared by physical currency, wallets and merchant transactions. */
public final class CrownBalance {
    private CrownBalance() {}
    public static long validate(long balance) {
        if (balance < 0) throw new IllegalArgumentException("Negative crown balance");
        return balance;
    }
    public static long deposit(long balance, long amount) {
        validate(balance); validate(amount);
        return Math.addExact(balance, amount);
    }
    public static long withdraw(long balance, long amount) {
        validate(balance); validate(amount);
        if (amount > balance) throw new IllegalArgumentException("Insufficient crowns");
        return balance - amount;
    }
    /** Reject rather than truncate withdrawals that cannot be represented in physical slots. */
    public static long physicalWithdrawal(long balance, long amount, int emptySlots) {
        if (emptySlots < 0 || amount > (long) emptySlots * 64)
            throw new IllegalArgumentException("Insufficient inventory capacity");
        return withdraw(balance, amount);
    }
}
