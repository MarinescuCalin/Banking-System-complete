# Banking-System-complete

Ca design patterns am folosit singleton factory builder observer si strategy.

Am adaugat clasele si functionalitatile implementate in etapa 1

Am adaugat functii ciot pentru comenzile noi pentru a putea testa

Am adaugat campurile noi in clasa User

Implementare comanda withdraw savings

Implementare comanda upgrade plan (am utilizat strategy pattern fiecare plan de cont reprezinta o strategie diferite de comision)

am adaugat si un static factory method pentru a crea tipul de strategie in functie de nume

Am adaugat un tip de Tranzactie noua, UpgradePlanTransaction,precum si CashWithdrawTransaction

Implementare comanda cashWithdrawal

Adaugare clasa Commerciante

Implementare cashback strategy pentru comercianti

Reparare addInterest

Adaugare tranzactie tip InterestRate
Implementare splitPayment cu sistem accept, reject; implementarea
sa am realizat-o folosind observer pattern; am creat o clasa
SplitPayment care contine informatiile despre conturi si suma
cat trebuie sa plateasca fiecare, clasa implementeaza interfata
PaymentObserver care expune metoda update; in clasa user
expun 2 metode, accept si update care apeleaza update din 
primul splitpayment din coada (acestia au o coada cu 
split payments)

De asemenea am implementat si builder pattern pentru clasa SplitPayment;
aceasta imi asigura ca nu se creeaza obiectul decat in momentul in care 
apelez build (moment in care am adaugat toate datele corespunzatoare
split payment-ului si ma asigur ca toti utilizatorii au balance
cel putin cat trebuie sa plateasca).

Am implementat comenzile accceptSplitPayment si rejectSplitPayment si 
am asigurat ca tranzactiile sunt intoarse in ordine crescatoare dupa
timestamp.

Am refactorizat codul pentru a il face mai clar si mai scurt.
Implementare clasa BusinessAccount.
Implementare comanda addNewBusinessAssociate.
Implementare comenzi changeSpendingLimit si changeDepositLimit.

Adaugare exceptie NotAuthorizedException.

Implementare comanda businessReport;

Adaugare transactie noua, Savings withdraw.
