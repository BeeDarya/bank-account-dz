enum Currency {

    RUB("RUB", "₽", 100),
    USD("USD", "$", 9_600),
    EUR("EUR", "€", 10_300),
    CNY("CNY", "¥", 1_320);

    private final String code;
    private final String symbol;
    private final long priceInRublesKopecks;

    Currency(String code, String symbol, long priceInRublesKopecks) {
        this.code = code;
        this.symbol = symbol;
        this.priceInRublesKopecks = priceInRublesKopecks;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getPriceInRublesKopecks() {
        return priceInRublesKopecks;
    }
}

final class Balance {

    private final Currency currency;
    private final long amountInKopecks;

    public Balance(Currency currency, long amountInKopecks) {
        if (currency == null) {
            throw new IllegalArgumentException("Валюта не может быть null");
        }
        if (amountInKopecks < 0) {
            throw new IllegalArgumentException("Баланс не может быть отрицательным");
        }
        this.currency = currency;
        this.amountInKopecks = amountInKopecks;
    }

    public Currency getCurrency() {
        return currency;
    }

    public long getAmountInKopecks() {
        return amountInKopecks;
    }

    public Balance add(long amountInKopecks) {
        if (amountInKopecks <= 0) {
            throw new IllegalArgumentException("Нельзя добавить отрицательную или нулевую сумму");
        }
        return new Balance(this.currency, this.amountInKopecks + amountInKopecks);
    }

    public Balance subtract(long amountInKopecks) {
        if (amountInKopecks <= 0) {
            throw new IllegalArgumentException("Нельзя снять отрицательную или нулевую сумму");
        }
        if (amountInKopecks > this.amountInKopecks) {
            throw new IllegalArgumentException("Недостаточно средств");
        }
        return new Balance(this.currency, this.amountInKopecks - amountInKopecks);
    }

    public Balance convertTo(Currency target) {
        if (target == null) {
            throw new IllegalArgumentException("Валюта не может быть null");
        }
        if (target == this.currency) {
            return this;
        }
        long inRubKopecks = this.amountInKopecks * this.currency.getPriceInRublesKopecks() / 100;
        long result = inRubKopecks * 100 / target.getPriceInRublesKopecks();
        return new Balance(target, result);
    }

    @Override
    public String toString() {
        long rubles = amountInKopecks / 100;
        long kopecks = amountInKopecks % 100;
        return rubles + "." + String.format("%02d", kopecks) + " " + currency.getSymbol();
    }
}

final class BankAccount {

    private static long nextId = 1;

    private final long id;
    private final String owner;
    private final Balance balance;
    private final long withdrawalLimit;

    public BankAccount(String owner, Currency currency, long withdrawalLimit) {
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("Имя владельца не может быть пустым");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Валюта не может быть null");
        }
        if (withdrawalLimit < 0) {
            throw new IllegalArgumentException("Лимит не может быть отрицательным");
        }
        this.id = nextId++;
        this.owner = owner;
        this.balance = new Balance(currency, 0);
        this.withdrawalLimit = withdrawalLimit;
    }

    private BankAccount(long id, String owner, Balance balance, long withdrawalLimit) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.withdrawalLimit = withdrawalLimit;
    }

    public long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public Balance getBalance() {
        return balance;
    }

    public long getWithdrawalLimit() {
        return withdrawalLimit;
    }

    public BankAccount deposit(long amount) {
        Balance newBalance = this.balance.add(amount);
        return new BankAccount(this.id, this.owner, newBalance, this.withdrawalLimit);
    }

    public BankAccount withdraw(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма снятия должна быть положительной");
        }
        if (amount > this.withdrawalLimit) {
            throw new IllegalArgumentException("Превышен лимит на снятие: " + this.withdrawalLimit + " коп.");
        }
        Balance newBalance = this.balance.subtract(amount);
        return new BankAccount(this.id, this.owner, newBalance, this.withdrawalLimit);
    }

    public static BankAccount[] transfer(BankAccount from, BankAccount to, long amount) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Счета не могут быть null");
        }
        if (from.id == to.id) {
            throw new IllegalArgumentException("Нельзя перевести самому себе");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной");
        }
        if (amount > from.withdrawalLimit) {
            throw new IllegalArgumentException("Сумма перевода превышает лимит отправителя");
        }

        Balance fromAfter = from.balance.subtract(amount);

        Balance converted = new Balance(from.balance.getCurrency(), amount)
                .convertTo(to.balance.getCurrency());

        Balance toAfter = to.balance.add(converted.getAmountInKopecks());

        BankAccount newFrom = new BankAccount(from.id, from.owner, fromAfter, from.withdrawalLimit);
        BankAccount newTo = new BankAccount(to.id, to.owner, toAfter, to.withdrawalLimit);

        return new BankAccount[]{newFrom, newTo};
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "owner='" + owner + '\'' +
                ", balance=" + balance +
                ", withdrawalLimit=" + withdrawalLimit + " коп." +
                '}';
    }
}

public class Main {

    public static void main(String[] args) {
        BankAccount alice = new BankAccount("Алиса", Currency.RUB, 10_000);
        BankAccount bob = new BankAccount("Боб", Currency.USD, 50_000);

        alice = alice.deposit(10_000);
        System.out.println("Алиса после пополнения: " + alice);

        Balance aliceInUsd = alice.getBalance().convertTo(Currency.USD);
        System.out.println("Баланс Алисы в USD: " + aliceInUsd);

        BankAccount[] result = BankAccount.transfer(alice, bob, 5_000);
        alice = result[0];
        bob = result[1];
        System.out.println("Алиса после перевода: " + alice);
        System.out.println("Боб после перевода:   " + bob);

        System.out.println();
        System.out.println("--- Проверяем корнер-кейсы ---");

        try {
            alice.deposit(-100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            alice.withdraw(20_000);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            alice.withdraw(9_000);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            new Balance(null, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        try {
            BankAccount.transfer(alice, alice, 100);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}