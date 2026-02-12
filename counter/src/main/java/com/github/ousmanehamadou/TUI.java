package com.github.ousmanehamadou;

import com.github.ousmanehamadou.shared.MoneyOrder;
import java.rmi.RemoteException;
import java.util.Scanner;
import jdk.jshell.spi.ExecutionControl;

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

    Press any key to continue
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

      switch (Integer.parseInt(input)) {
        case 1 -> handleMoneySend(null);
        case 2 ->
            throw new ExecutionControl.NotImplementedException(
                "CASHING    - Withdrawal at the counter");
        case 3 ->
            throw new ExecutionControl.NotImplementedException(
                "CANCELLING - Void or reverse a transaction");
        case 4 ->
            throw new ExecutionControl.NotImplementedException(
                "TRACKING   - Check status (Paid / Pending)");
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
    System.out.flush();
    if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
      try {
        moneyOrderService.issuing(sender, recipient, amount);
      } catch (RemoteException e) {
        System.out.print(getServerUnreachableMessage());
      }
      System.out.printf(successMsg, 0);
    } else {
      System.out.print(getAbortMessage());
    }
    sc.nextLine();
    System.out.flush();
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

        Press Any Key To Returning To Main Menu...
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
}
