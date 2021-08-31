# EMT-KRA-Connector
A Spring Boot API For consulting an E-Slip and Posting Payments to KRA.
Endpoint for consulting :: /kra/consult/ + eslipnumber
Endpoint for posting a payment :: /kra/pay/ + eslipnumber + / +meansofpayment
The requests are sent to KRA and a response in form of an XML is sent back to the API.
Sample Response after Consulting:

<ns2:ESLIP xmlns:ns2="http://impl.facade.pg.kra.tcs.com/">
<ESLIPDETAILS>
<AmountPerTax>20856</AmountPerTax>
<TaxCode>3200</TaxCode>
<TaxComponent>Income Tax - Withholding</TaxComponent>
<TaxHead>N/A</TaxHead>
<TaxPeriod>2021-07-01</TaxPeriod>
</ESLIPDETAILS>
<ESLIPHEADER>
<Currency>KES</Currency>
<DocRefNumber>N/A</DocRefNumber>
<EslipNumber>2020210001969676</EslipNumber>
<PaymentAdviceDate>2021-07-08T17:09:58</PaymentAdviceDate>
<SlipPaymentCode>N/A</SlipPaymentCode>
<SystemCode>PG</SystemCode>
<TaxpayerFullName>GIZ GMBH</TaxpayerFullName>
<TaxpayerPin>P051092963D</TaxpayerPin>
<TotalAmount>20856</TotalAmount>
</ESLIPHEADER>
<HASHCODE>37399a42b6683bfb9d22970969b90bb46fc6051942bf191de0e004526079f194</HASHCODE>
<RESULT>
<Status>VALID</Status>
<Remarks>REQUESTED/CONSULTED E SLIP NUMBER (PRN) EXISTS IN THE SYSTEM</Remarks>
</RESULT>
</ns2:ESLIP>

::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
This response is saved in Oracle database :: table name  custom.eslip_data


Sample responsse after posting a payment:
------------------------------------------
<PAYMENTS>
<Message>THE PAYMENT FOR REQUESTED PRN HAS ALREADY BEEN PROCESSED</Message>
<PaymentNumber>2020210002163952</PaymentNumber>
<ResponseCode>NOK</ResponseCode>
<Status>60003</Status>
</PAYMENTS>

This payment XML is generated when sending a payment to KRA::
-------------------------------------------------------------
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<CCRSPAYMENT>
    <PAYMENT>
        <PAYMENTHEADER>
            <SystemCode>PG</SystemCode>
            <BranchCode>54001</BranchCode>
            <BankTellerId>12</BankTellerId>
            <BankTellerName>TEST VCB</BankTellerName>
            <PaymentMode>1</PaymentMode>
            <MeansOfPayment>1</MeansOfPayment>
            <RemitterId>12345</RemitterId>
            <RemitterName>VCB TEST</RemitterName>
            <EslipNumber>1020210000400204</EslipNumber>
            <SlipPaymentCode></SlipPaymentCode>
            <PaymentAdviceDate></PaymentAdviceDate>
            <PaymentReference></PaymentReference>
            <TaxpayerPin></TaxpayerPin>
            <TaxpayerFullName></TaxpayerFullName>
            <TotalAmount></TotalAmount>
            <DocRefNumber></DocRefNumber>
            <DateOfCollection>2021-08-31T17:46:58</DateOfCollection>
            <Currency>KES</Currency>
            <CashAmount></CashAmount>
            <ChequesAmount>0</ChequesAmount>
        </PAYMENTHEADER>
        <hashCode>10202100004002041254nQ9KIrHD+Mf3CHRqrUPPUA==</hashCode>
    </PAYMENT>
</CCRSPAYMENT>

After posting a payment the results are stored in the database (Oracle) :: table name :: custom.paymentdetails

###THE END ###
