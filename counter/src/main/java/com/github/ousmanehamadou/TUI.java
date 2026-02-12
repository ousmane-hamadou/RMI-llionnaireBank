package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import com.github.ousmanehamadou.shared.Status;
import com.github.ousmanehamadou.shared.exception.DomainException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class TUI {
  private static final Scanner sc = new Scanner(System.in);
  private static final String menuOptions =
      """
          [1] ISSUING    - Send a new Money Order
          [2] CASHING    - Withdrawal at the counter
          [3] CANCELLING - Void or reverse a transaction
          [4] TRACKING   - Check status (Paid / Pending)
        """;
  private static final String border =
      "==========================================================\n";
  private static final String successMsg =
      """
    ==========================================================
       ✅  TRANSACTION SUCCESSFUL  ✅
    ==========================================================
    MTCN (Tracking Number) : %d
    Status                 : PENDING COLLECTION

    Please share the MTCN ONLY with the recipient.

    Press Enter to continue
    ==========================================================
    """;

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    //    System.out.flush(); // Ensures the command is sent immediately
  }

  private static String getWelcomeMessage() {
    return border
        + "    \uD83D\uDCB0  WELCOME TO RMI-llionaireBank TERMINAL  \uD83D\uDCB0    \n"
        + border
        + "Status: CONNECTED TO DISTRIBUTED CLUSTER\n"
        + "Ready for Banking Operations:\n\n"
        + menuOptions
        + "Security Note: Never share your MTCN (Tracking Number).\n"
        + border
        + "Enter your choice (1-4) or 'exit' to quit > ";
  }

  public static void run(MoneyOrder moneyOrderService) throws Exception {
    while (true) {
      clearScreen();
      System.out.print(getWelcomeMessage());
      System.out.flush();

      String input = sc.nextLine().strip();

      if (input.equalsIgnoreCase("exit")) {
        return;
      }

      int selection = 0;

      try {
        selection = Integer.parseInt(input);
      } catch (NumberFormatException e) {
        continue;
      }
      if (selection > 4) continue;
      switch (selection) {
        case 1 -> handleMoneySend(moneyOrderService);
        case 2 -> handleCashing(moneyOrderService);
        case 3 -> handleCancellation(moneyOrderService);
        case 4 -> handleTracking(moneyOrderService);
      }
    }
  }

  private static void handleMoneySend(MoneyOrder moneyOrderService) {
    clearScreen();
    System.out.print(getIssuingForm(null, null, -1, false));
    System.out.flush();
    String sender = sc.nextLine().trim();

    clearScreen();
    System.out.flush();
    System.out.print(getIssuingForm(sender, null, -1, false));
    System.out.flush();
    String recipient = sc.nextLine();

    clearScreen();
    System.out.flush();
    System.out.print(getIssuingForm(sender, recipient, -1, false));
    System.out.flush();
    int amount = Integer.parseInt(sc.nextLine().strip());

    clearScreen();
    System.out.flush();
    System.out.print(getIssuingForm(sender, recipient, amount, false));
    System.out.flush();

    clearScreen();
    System.out.flush();
    System.out.print(getIssuingForm(sender, recipient, amount, true));
    System.out.flush();

    String confirm = sc.nextLine().trim();

    clearScreen();
    if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
      try {
        var order = moneyOrderService.issuing(sender, recipient, amount);
        System.out.printf(successMsg, order.ref());
      } catch (RemoteException e) {
        System.out.print(getServerUnreachableMessage());
      }
    } else {
      System.out.print(getAbortMessage());
    }
    System.out.flush();
    sc.nextLine();
  }

  private static String getIssuingForm(
      String sender, String recipient, int amount, boolean confirm) {
    String prompt = "Enter ";
    String pSender = sender;
    String pRecipient = recipient;
    String pAmount = String.valueOf(amount);

    if (sender == null) {
      prompt += "Sender Full Name";
      pSender = "";
      pRecipient = "";
      pAmount = "";
    } else if (recipient == null) {
      prompt += "Recipient Full Name";
      pRecipient = "";
      pAmount = "";
    } else if (amount == -1) {
      prompt += "Amount to Transfer";
      pAmount = "";
    }

    if (confirm) {
      prompt = "Confirm issuance? (YES/NO)";
    }

    String border = "----------------------------------------------------------";

    return """
        %s
           •  NEW MONEY ORDER ISSUANCE  •
        %s
        Please provide the transaction details:

          1. Sender               : %s
          2. Recipient            : %s
          3. Amount to Transfer   : %s

        Note: A 2%% service fee will be applied to the total.
        %s
        %s >\s"""
        .formatted(border, border, pSender, pRecipient, pAmount, border, prompt);
  }

  public static String getAbortMessage() {
    String border = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    return """
        %s
           ⚠️  TRANSACTION ABORTED  ⚠️
        %s
        Action: ISSUANCE CANCELLED
        Status: NO FUNDS WERE PROCESSED

        Press Enter To Returning To Main Menu...
        %s
        """
        .formatted(border, border, border);
  }

  public static String getServerUnreachableMessage() {
    String border = "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";

    return """
        %s
           ❌  SERVER CONNECTION FAILURE  ❌
        %s
        Unable to reach the Banking Cluster

        Possible reasons:
          - The remote node is OFFLINE
          - Network TIMEOUT (Check your VPN/Firewall)
        %s
        """
        .formatted(border, border, border);
  }

  private static void handleTracking(MoneyOrder moneyOrder) {
    clearScreen();
    System.out.print(getTrackingSearchPage(""));
    System.out.flush();

    String input = sc.nextLine().strip();

    clearScreen();
    System.out.print(getTrackingSearchPage(input));
    System.out.flush();

    sc.nextLine();

    clearScreen();
    System.out.flush();
    try {
      var order = moneyOrder.tracking(Integer.parseInt(input));
      System.out.printf(
          getTrackingResultPage(
              String.valueOf(order.ref()),
              order.by(),
              order.to(),
              order.amount(),
              order.status().getClass().getSimpleName().toUpperCase()));
      System.out.flush();
      sc.nextLine();
    } catch (DomainException.OrderNotFoundException e) {
      System.out.print(getReferenceNotFoundPage(input));
      System.out.flush();
      sc.nextLine();
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
    clearScreen();
    System.out.flush();
  }

  public static String getTrackingSearchPage(String input) {
    String border = "----------------------------------------------------------";
    String prompt = "Enter Order Reference";

    if (!input.isBlank()) {
      prompt = "Confirm Order Reference? (YES/NO)";
    }

    return """
        %s
           \uD83D\uDD0D  MONEY ORDER TRACKING SYSTEM  \uD83D\uDD0D
        %s
        Please enter the Control Number to locate the transaction:

          Order Reference : %s
        %s
        %s>\s"""
        .formatted(border, border, input, border, prompt);
  }

  public static String getTrackingResultPage(
      String ref, String sender, String recipient, int amount, String status) {
    String border = "==========================================================";

    return """
        %s
           \uD83D\uDACA  TRANSACTION STATUS REPORT  \uD83D\uDACA
        %s
        Reference : %s

          - SENDER      : %s
          - RECIPIENT   : %s
          - AMOUNT      : %d
          - CURRENT STATUS : %s

        [LEGEND]:
          Cashed - Ready for cashing at any branch.
          AwaitingPayout - Funds already paid to recipient.
          Cancelled - Voided by sender or system.
        %s
        Press Enter to return to menu..."""
        .formatted(border, border, ref, sender, recipient, amount, status, border);
  }

  public static String getReferenceNotFoundPage(String ref) {
    String alertBorder = "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!";

    return """
        %s
           ⚠️  TRANSACTION REFERENCE NOT FOUND  ⚠️
        %s

        The REF provided does not match any record in our system.

        ENTERED REF : %s
        ERROR STATUS : INVALID_REFERENCE

        [ACTION]: Please verify the receipt and try again.
        %s
        Press Enter to return to Main Menu>\s"""
        .formatted(alertBorder, alertBorder, ref, alertBorder);
  }

  public static String getCashingInputPage() {
    String border = "----------------------------------------------------------";

    return """
        %s
           \uD83D\uDCB5  MONEY ORDER CASHING (WITHDRAWAL)  \uD83D\uDCB5
        %s
        Please enter the REF provided by the recipient:

        %s
        Enter REF to verify >\s"""
        .formatted(border, border, border);
  }

  public static String getCancellationPrompt() {
    String border = "----------------------------------------------------------";

    return """
        %s
             TRANSACTION CANCELLATION (VOID)
        %s
        Please enter the MTCN of the mandate to be cancelled:

        Warning: This action will permanently VOID the money order.
        %s
        Enter REF to Cancel >\s"""
        .formatted(border, border, border);
  }

  private static void handleCashing(MoneyOrder moneyOrder) {
    clearScreen();
    System.out.flush();

    System.out.print(getCashingInputPage());

    int ref = Integer.parseInt(sc.nextLine());
    clearScreen();
    System.out.flush();
    try {
      var ret = moneyOrder.cashing(ref);
      System.out.println(ret);
      var status = ret.status();
      var sref = String.valueOf(ref);

      switch (status) {
        case Status.Cashed ignored when ret.done() ->
            System.out.print(getCashingSuccessMessage(sref));
        case Status.Cancelled ignored -> System.out.print(getCancelSuccessMessage(sref));
        default -> System.out.print(getAlreadyCashedMessage(String.valueOf(ref)));
      }
    } catch (DomainException.OrderNotFoundException e) {
      System.out.println("---------");
      System.out.print(getReferenceNotFoundPage(String.valueOf(ref)));
    } catch (RemoteException e) {
      System.out.println("(((((((((((((((((((((((((((((((((");
      System.out.flush();
      throw new RuntimeException(e);
    }
    System.out.flush();
    sc.nextLine();
  }

  public static String getCashingSuccessMessage(String ref) {
    return """
        ==========================================================
           \uD83D\uDCB0  PAYMENT CONFIRMED (PAID)  \uD83D\uDCB0
        ==========================================================
        Reference  : %s
        Status     : COLLECTED

        The transaction is now closed in the cluster.
        ==========================================================
        """
        .formatted(ref);
  }

  public static String getAlreadyCashedMessage(String ref) {
    return """
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
           ⚠️  ERROR: ALREADY COLLECTED  ⚠️
        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        The money order (%s) has already been cashed.

        Status: PAID
        Note: Double payment is strictly prohibited.

        !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        """
        .formatted(ref);
  }

  public static String getCancelSuccessMessage(String ref) {
    return """
        ----------------------------------------------------------
           ✅  TRANSACTION VOIDED  ✅
        ----------------------------------------------------------
        REF    : %s
        Status : CANCELLED
        Result : Funds are no longer available for cashing.
        ----------------------------------------------------------
        """
        .formatted(ref);
  }

  public static String getInvalidMtcnMessage(String ref) {
    return """
        **********************************************************
           ❌  INVALID REFERENCE NUMBER  ❌
        **********************************************************
        Reference [%s] was not found in the distributed ledger.

        **********************************************************
        """
        .formatted(ref);
  }

  public static void handleCancellation(MoneyOrder moneyOrder) {
    clearScreen();
    System.out.flush();

    System.out.printf(getCancellationPrompt());
    System.out.flush();

    int ref = Integer.parseInt(sc.nextLine().strip());

    try {
      clearScreen();
      Status status = moneyOrder.cancelling(ref);
      System.out.print(getCashingSuccessMessage(String.valueOf(ref)));

      sc.nextLine();
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    } catch (DomainException.OrderNotFoundException e) {
      System.out.println(getInvalidMtcnMessage(String.valueOf(ref)));
      sc.nextLine();
    }
  }
}
