SET SERVEROUTPUT ON;

-- INSURANCE POLICY CLAIMS SYSTEM
-- Run while connected as INSURANCE.

CREATE TABLE Customers2 (
    Address VARCHAR2(100) PRIMARY KEY,
    L_Name VARCHAR2(50)
);

CREATE TABLE Customer1 (
    Person_ID VARCHAR2(10) PRIMARY KEY,
    F_Name VARCHAR2(50),
    DOB DATE,
    Email VARCHAR2(100),
    Aadhar VARCHAR2(12),
    Address VARCHAR2(100),
    CONSTRAINT fk_customer_address FOREIGN KEY (Address) REFERENCES Customers2(Address)
);

CREATE TABLE Agent2 (
    Commission_Rate NUMBER(5,2) PRIMARY KEY,
    Email VARCHAR2(100)
);

CREATE TABLE Agent1 (
    Person_ID VARCHAR2(10) PRIMARY KEY,
    F_Name VARCHAR2(50),
    L_Name VARCHAR2(50),
    DOB DATE,
    Commission_Rate NUMBER(5,2),
    CONSTRAINT fk_agent_commission FOREIGN KEY (Commission_Rate) REFERENCES Agent2(Commission_Rate)
);

CREATE TABLE Policy2 (
    Start_Date DATE PRIMARY KEY,
    Expiry_Date DATE
);

CREATE TABLE Policy3 (
    Audit_ID VARCHAR2(10) PRIMARY KEY,
    Premium_Amt NUMBER(12,2)
);

CREATE TABLE Policy1 (
    P_No VARCHAR2(10) PRIMARY KEY,
    Start_Date DATE,
    Audit_ID VARCHAR2(10),
    CONSTRAINT fk_policy_start FOREIGN KEY (Start_Date) REFERENCES Policy2(Start_Date),
    CONSTRAINT fk_policy_audit FOREIGN KEY (Audit_ID) REFERENCES Policy3(Audit_ID)
);

CREATE TABLE Asset2 (
    Descr VARCHAR2(50) PRIMARY KEY,
    Est_Value NUMBER(12,2)
);

CREATE TABLE Asset1 (
    Asset_ID VARCHAR2(10) PRIMARY KEY,
    Descr VARCHAR2(50),
    P_Date DATE,
    CONSTRAINT fk_asset_descr FOREIGN KEY (Descr) REFERENCES Asset2(Descr)
);

CREATE TABLE Purchase1 (
    P_No VARCHAR2(10),
    Purchase_Date DATE,
    CONSTRAINT pk_purchase1 PRIMARY KEY (P_No, Purchase_Date),
    CONSTRAINT fk_purchase1_policy FOREIGN KEY (P_No) REFERENCES Policy1(P_No)
);

CREATE TABLE Purchase2 (
    P_No VARCHAR2(10) PRIMARY KEY,
    Person_ID VARCHAR2(10),
    CONSTRAINT fk_purchase2_policy FOREIGN KEY (P_No) REFERENCES Policy1(P_No),
    CONSTRAINT fk_purchase2_customer FOREIGN KEY (Person_ID) REFERENCES Customer1(Person_ID)
);

CREATE TABLE Covers2 (
    P_No VARCHAR2(10) PRIMARY KEY,
    Coverage_Start_Date DATE,
    CONSTRAINT fk_covers2_policy FOREIGN KEY (P_No) REFERENCES Policy1(P_No)
);

CREATE TABLE Covers1 (
    P_No VARCHAR2(10),
    Asset_ID VARCHAR2(10),
    CONSTRAINT pk_covers1 PRIMARY KEY (P_No, Asset_ID),
    CONSTRAINT fk_covers1_policy FOREIGN KEY (P_No) REFERENCES Policy1(P_No),
    CONSTRAINT fk_covers1_asset FOREIGN KEY (Asset_ID) REFERENCES Asset1(Asset_ID)
);

CREATE TABLE Audit2 (
    Auditor_Name VARCHAR2(100) PRIMARY KEY,
    Audit_Date DATE
);

CREATE TABLE Audit1 (
    Audit_ID VARCHAR2(10) PRIMARY KEY,
    Auditor_Name VARCHAR2(100),
    Audit_Status VARCHAR2(30),
    CONSTRAINT fk_audit1_name FOREIGN KEY (Auditor_Name) REFERENCES Audit2(Auditor_Name)
);

CREATE TABLE Claim1 (
    Incident_Date DATE PRIMARY KEY,
    Claim_Status VARCHAR2(30)
);

CREATE TABLE Claim2B (
    Descr VARCHAR2(50) PRIMARY KEY,
    Claimed_Amt NUMBER(12,2)
);

CREATE TABLE Claim2A (
    Claim_ID VARCHAR2(10) PRIMARY KEY,
    P_No VARCHAR2(10),
    Incident_Date DATE,
    Descr VARCHAR2(50),
    CONSTRAINT fk_claim2a_policy FOREIGN KEY (P_No) REFERENCES Policy1(P_No),
    CONSTRAINT fk_claim2a_date FOREIGN KEY (Incident_Date) REFERENCES Claim1(Incident_Date),
    CONSTRAINT fk_claim2a_descr FOREIGN KEY (Descr) REFERENCES Claim2B(Descr)
);

CREATE TABLE ServiceProvider1 (
    Provider_ID VARCHAR2(10) PRIMARY KEY,
    Name VARCHAR2(100),
    Bank_Acc VARCHAR2(30)
);

CREATE TABLE ServiceProvider2 (
    Provider_ID VARCHAR2(10),
    Phone VARCHAR2(30),
    CONSTRAINT pk_serviceprovider2 PRIMARY KEY (Provider_ID, Phone),
    CONSTRAINT fk_serviceprovider2_provider FOREIGN KEY (Provider_ID) REFERENCES ServiceProvider1(Provider_ID)
);

CREATE TABLE Payment2 (
    P_Method VARCHAR2(30) PRIMARY KEY,
    Amt_Paid NUMBER(12,2)
);

CREATE TABLE Payment1 (
    Claim_ID VARCHAR2(10),
    Payment_ID VARCHAR2(10),
    P_Date DATE,
    P_Method VARCHAR2(30),
    CONSTRAINT pk_payment1 PRIMARY KEY (Claim_ID, Payment_ID),
    CONSTRAINT fk_payment1_claim FOREIGN KEY (Claim_ID) REFERENCES Claim2A(Claim_ID),
    CONSTRAINT fk_payment1_method FOREIGN KEY (P_Method) REFERENCES Payment2(P_Method)
);

CREATE TABLE Involves2 (
    Provider_ID VARCHAR2(10) PRIMARY KEY,
    Asset_ID VARCHAR2(10),
    CONSTRAINT fk_involves2_provider FOREIGN KEY (Provider_ID) REFERENCES ServiceProvider1(Provider_ID),
    CONSTRAINT fk_involves2_asset FOREIGN KEY (Asset_ID) REFERENCES Asset1(Asset_ID)
);

CREATE TABLE Involves1 (
    Claim_ID VARCHAR2(10),
    Provider_ID VARCHAR2(10),
    Parts_Cost NUMBER(12,2),
    CONSTRAINT pk_involves1 PRIMARY KEY (Claim_ID, Provider_ID),
    CONSTRAINT fk_involves1_claim FOREIGN KEY (Claim_ID) REFERENCES Claim2A(Claim_ID),
    CONSTRAINT fk_involves1_provider FOREIGN KEY (Provider_ID) REFERENCES ServiceProvider1(Provider_ID)
);

-- DATA
INSERT INTO Customers2 VALUES ('101 Maple St', 'Sterling');
INSERT INTO Customers2 VALUES ('202 Maple St', 'Mercer');
INSERT INTO Customers2 VALUES ('404 Elm Blvd', 'Sterling');
INSERT INTO Customers2 VALUES ('505 Ceder Ln', 'Solberg');

INSERT INTO Customer1 VALUES ('C001','Eleanor',DATE '1990-05-10','eleanor@email.com','123456789111','101 Maple St');
INSERT INTO Customer1 VALUES ('C002','Tobias',DATE '1988-11-20','tobias@email.com','111122223333','202 Maple St');
INSERT INTO Customer1 VALUES ('C003','Amara',DATE '1992-06-15','amara@email.com','444455556666','101 Maple St');
INSERT INTO Customer1 VALUES ('C004','Kieran',DATE '1985-02-28','kieran@email.com','999988887777','404 Elm Blvd');
INSERT INTO Customer1 VALUES ('C005','Amara',DATE '1999-09-09','amara@email.com','123498765678','505 Ceder Ln');

INSERT INTO Agent2 VALUES (0.11,'tierC@email.com');
INSERT INTO Agent2 VALUES (0.12,'tierB@omal.com');
INSERT INTO Agent2 VALUES (0.15,'tierA@email.com');
INSERT INTO Agent2 VALUES (0.18,'tierA@email.com');

INSERT INTO Agent1 VALUES ('A001','Jasper','Fjord',DATE '1980-01-15',0.15);
INSERT INTO Agent1 VALUES ('A002','Cassidy','Blake',DATE '1985-06-20',0.12);
INSERT INTO Agent1 VALUES ('A003','Jasper','Bjord',DATE '1986-07-21',0.18);
INSERT INTO Agent1 VALUES ('A004','Eleanor','Sterling',DATE '1990-05-10',0.15);
INSERT INTO Agent1 VALUES ('A005','Kieran','Ostiea',DATE '1985-06-20',0.11);

INSERT INTO Policy2 VALUES (DATE '2026-01-02',DATE '2027-01-02');
INSERT INTO Policy2 VALUES (DATE '2026-02-01',DATE '2027-02-01');
INSERT INTO Policy2 VALUES (DATE '2026-03-02',DATE '2027-03-02');
INSERT INTO Policy2 VALUES (DATE '2026-04-05',DATE '2027-04-05');

INSERT INTO Policy3 VALUES ('AU01',5000);
INSERT INTO Policy3 VALUES ('AU02',8000);
INSERT INTO Policy3 VALUES ('AU03',3000);

INSERT INTO Policy1 VALUES ('P101',DATE '2026-01-02','AU01');
INSERT INTO Policy1 VALUES ('P102',DATE '2026-02-01','AU01');
INSERT INTO Policy1 VALUES ('P103',DATE '2026-03-02','AU02');
INSERT INTO Policy1 VALUES ('P104',DATE '2026-02-01','AU02');
INSERT INTO Policy1 VALUES ('P105',DATE '2026-04-05','AU03');

INSERT INTO Asset2 VALUES ('Laptop',150000);
INSERT INTO Asset2 VALUES ('Tablet',160000);
INSERT INTO Asset2 VALUES ('Sedan',150000);
INSERT INTO Asset2 VALUES ('Apartment',180000);

INSERT INTO Asset1 VALUES ('AS01','Laptop',DATE '2025-10-10');
INSERT INTO Asset1 VALUES ('AS02','Tablet',DATE '2025-10-10');
INSERT INTO Asset1 VALUES ('AS03','Sedan',DATE '2026-11-12');
INSERT INTO Asset1 VALUES ('AS04','Apartment',DATE '2024-11-10');
INSERT INTO Asset1 VALUES ('AS05','Laptop',DATE '2021-08-22');

INSERT INTO Purchase1 VALUES ('P101',DATE '2025-12-28');
INSERT INTO Purchase1 VALUES ('P101',DATE '2026-01-25');
INSERT INTO Purchase1 VALUES ('P102',DATE '2026-01-25');
INSERT INTO Purchase1 VALUES ('P103',DATE '2026-02-20');
INSERT INTO Purchase1 VALUES ('P105',DATE '2026-04-28');

INSERT INTO Purchase2 VALUES ('P101','C001');
INSERT INTO Purchase2 VALUES ('P102','C002');
INSERT INTO Purchase2 VALUES ('P103','C003');
INSERT INTO Purchase2 VALUES ('P105','C005');

INSERT INTO Covers1 VALUES ('P101','AS01');
INSERT INTO Covers1 VALUES ('P101','AS02');
INSERT INTO Covers1 VALUES ('P102','AS01');
INSERT INTO Covers1 VALUES ('P102','AS04');
INSERT INTO Covers1 VALUES ('P103','AS05');

INSERT INTO Covers2 VALUES ('P101',DATE '2026-01-01');
INSERT INTO Covers2 VALUES ('P102',DATE '2026-02-01');
INSERT INTO Covers2 VALUES ('P103',DATE '2026-03-01');

INSERT INTO Audit2 VALUES ('Silas Vance',DATE '2026-01-10');
INSERT INTO Audit2 VALUES ('Cora Mills',DATE '2026-02-05');
INSERT INTO Audit2 VALUES ('Elias Thorne',DATE '2026-03-20');

INSERT INTO Audit1 VALUES ('AU01','Silas Vance','Completed');
INSERT INTO Audit1 VALUES ('AU02','Silas Vance','Pending');
INSERT INTO Audit1 VALUES ('AU03','Elias Thorne','Pending');
INSERT INTO Audit1 VALUES ('AU04','Cora Mills','Pending');
INSERT INTO Audit1 VALUES ('AU05','Cora Mills','Pending');

INSERT INTO Claim1 VALUES (DATE '2026-03-15','Approved');
INSERT INTO Claim1 VALUES (DATE '2026-04-10','Pending');
INSERT INTO Claim1 VALUES (DATE '2026-06-01','Pending');

INSERT INTO Claim2B VALUES ('Bumper dent',15000);
INSERT INTO Claim2B VALUES ('Screen broken',5000);
INSERT INTO Claim2B VALUES ('Water leak',25000);

INSERT INTO Claim2A VALUES ('CL01','P101',DATE '2026-03-15','Screen broken');
INSERT INTO Claim2A VALUES ('CL02','P102',DATE '2026-04-10','Bumper dent');
INSERT INTO Claim2A VALUES ('CL03','P103',DATE '2026-03-15','Water leak');
INSERT INTO Claim2A VALUES ('CL04','P104',DATE '2026-04-10','Screen broken');
INSERT INTO Claim2A VALUES ('CL05','P102',DATE '2026-06-01','Water leak');

INSERT INTO ServiceProvider1 VALUES ('SP01','Prime Jewellers','1122334455');
INSERT INTO ServiceProvider1 VALUES ('SP02','Metro Auto','5544332211');
INSERT INTO ServiceProvider1 VALUES ('SP03','Alex Plumbing','1122334455');
INSERT INTO ServiceProvider1 VALUES ('SP04','Rapid Electronics','9988776655');
INSERT INTO ServiceProvider1 VALUES ('SP05','Prime Jewellers','2233445566');

INSERT INTO ServiceProvider2 VALUES ('SP01','981-0001');
INSERT INTO ServiceProvider2 VALUES ('SP01','981-0002');
INSERT INTO ServiceProvider2 VALUES ('SP02','987-803');
INSERT INTO ServiceProvider2 VALUES ('SP03','981-0004');
INSERT INTO ServiceProvider2 VALUES ('SP03','981-0099');
INSERT INTO ServiceProvider2 VALUES ('SP04','981-005');
INSERT INTO ServiceProvider2 VALUES ('SP05','981-0004');
INSERT INTO ServiceProvider2 VALUES ('SP05','981-0006');

INSERT INTO Payment2 VALUES ('Wire',140000);
INSERT INTO Payment2 VALUES ('App',4500);
INSERT INTO Payment2 VALUES ('Cheque',4500);

INSERT INTO Payment1 VALUES ('CL01','PM01',DATE '2026-03-20','Wire');
INSERT INTO Payment1 VALUES ('CL01','PM02',DATE '2026-03-26','App');
INSERT INTO Payment1 VALUES ('CL02','PM01',DATE '2026-03-26','Wire');
INSERT INTO Payment1 VALUES ('CL02','PM02',DATE '2026-04-10','Cheque');
INSERT INTO Payment1 VALUES ('CL02','PM03',DATE '2026-04-10','Wire');
INSERT INTO Payment1 VALUES ('CL03','PM02',DATE '2026-04-10','Wire');

INSERT INTO Involves2 VALUES ('SP01','AS01');
INSERT INTO Involves2 VALUES ('SP02','AS02');
INSERT INTO Involves2 VALUES ('SP03','AS03');

-- Page 18 of the uploaded DA-1 shows CL04/SP03 Parts_Cost = 3000.
INSERT INTO Involves1 VALUES ('CL01','SP01',3000);
INSERT INTO Involves1 VALUES ('CL01','SP02',1500);
INSERT INTO Involves1 VALUES ('CL02','SP01',4500);
INSERT INTO Involves1 VALUES ('CL03','SP02',8000);
INSERT INTO Involves1 VALUES ('CL04','SP03',3000);

COMMIT;

-- PL/SQL
CREATE OR REPLACE PROCEDURE add_customer(
    p_person_id IN VARCHAR2,
    p_f_name IN VARCHAR2,
    p_dob IN DATE,
    p_email IN VARCHAR2,
    p_aadhar IN VARCHAR2,
    p_address IN VARCHAR2
)
IS
BEGIN
    INSERT INTO Customer1 VALUES (p_person_id,p_f_name,p_dob,p_email,p_aadhar,p_address);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Customer added successfully');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

CREATE OR REPLACE PROCEDURE register_claim(
    p_claim_id IN VARCHAR2,
    p_p_no IN VARCHAR2,
    p_incident_date IN DATE,
    p_descr IN VARCHAR2,
    p_claim_status IN VARCHAR2
)
IS
BEGIN
    INSERT INTO Claim1 VALUES (p_incident_date,p_claim_status);
    INSERT INTO Claim2A VALUES (p_claim_id,p_p_no,p_incident_date,p_descr);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Claim registered successfully');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

CREATE OR REPLACE PROCEDURE make_payment(
    p_claim_id IN VARCHAR2,
    p_payment_id IN VARCHAR2,
    p_date IN DATE,
    p_method IN VARCHAR2
)
IS
BEGIN
    INSERT INTO Payment1 VALUES (p_claim_id,p_payment_id,p_date,p_method);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Payment recorded successfully');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

CREATE OR REPLACE FUNCTION get_claim_amount(p_claim_id IN VARCHAR2)
RETURN NUMBER
IS
    v_amount NUMBER;
BEGIN
    SELECT B.Claimed_Amt INTO v_amount
    FROM Claim2A A JOIN Claim2B B ON A.Descr=B.Descr
    WHERE A.Claim_ID=p_claim_id;
    RETURN v_amount;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END;
/

CREATE OR REPLACE TRIGGER check_policy_expiry
BEFORE INSERT ON Claim2A
FOR EACH ROW
DECLARE
    v_expiry_date DATE;
BEGIN
    SELECT P2.Expiry_Date INTO v_expiry_date
    FROM Policy1 P1 JOIN Policy2 P2 ON P1.Start_Date=P2.Start_Date
    WHERE P1.P_No=:NEW.P_No;
    IF :NEW.Incident_Date > v_expiry_date THEN
        RAISE_APPLICATION_ERROR(-20001,'Claim cannot be registered after policy expiry');
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE display_claims
IS
    CURSOR claim_cursor IS
        SELECT A.Claim_ID,A.P_No,A.Incident_Date,A.Descr,B.Claimed_Amt,C.Claim_Status
        FROM Claim2A A
        JOIN Claim2B B ON A.Descr=B.Descr
        JOIN Claim1 C ON A.Incident_Date=C.Incident_Date;
    v_claim_id Claim2A.Claim_ID%TYPE;
    v_p_no Claim2A.P_No%TYPE;
    v_incident_date Claim2A.Incident_Date%TYPE;
    v_descr Claim2A.Descr%TYPE;
    v_amount Claim2B.Claimed_Amt%TYPE;
    v_status Claim1.Claim_Status%TYPE;
BEGIN
    OPEN claim_cursor;
    LOOP
        FETCH claim_cursor INTO v_claim_id,v_p_no,v_incident_date,v_descr,v_amount,v_status;
        EXIT WHEN claim_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(v_claim_id||' | '||v_p_no||' | '||TO_CHAR(v_incident_date,'DD-MON-YYYY')||' | '||v_descr||' | '||v_amount||' | '||v_status);
    END LOOP;
    CLOSE claim_cursor;
END;
/

COMMIT;

PROMPT ============================================================
PROMPT DATABASE SETUP COMPLETED
PROMPT ============================================================
