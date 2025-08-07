package com.example.loanappandroid;

/**
 * Created by Md Arifur Rahaman on 17/1/22.
 * Copyright © 2017 Mutual Trust Bank Limited
 */
public interface OtpReceivedInterface {
    void onOtpReceived(String otp);
    void onOtpTimeout();
}
