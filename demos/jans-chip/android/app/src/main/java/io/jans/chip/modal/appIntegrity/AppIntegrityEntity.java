package io.jans.chip.modal.appIntegrity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "APP_INTEGRITY")
public class AppIntegrityEntity {
    @NonNull
    @PrimaryKey
    @SerializedName("SNO")
    private String sno;
    @ColumnInfo(name = "APP_INTEGRITY")
    private String appIntegrity;
    @ColumnInfo(name = "DEVICE_INTEGRITY")
    private String deviceIntegrity;
    @ColumnInfo(name = "APP_LICENSING_VERDICT")
    private String appLicensingVerdict;
    @ColumnInfo(name = "REQUEST_PACKAGE_NAME")
    private String requestPackageName;
    @ColumnInfo(name = "NONCE")
    private String nonce;
    @ColumnInfo(name = "ERROR")
    private String error;

    public AppIntegrityEntity(String sno, String appIntegrity, String deviceIntegrity, String appLicensingVerdict, String requestPackageName, String nonce, String error) {
        this.sno = sno;
        this.appIntegrity = appIntegrity;
        this.deviceIntegrity = deviceIntegrity;
        this.appLicensingVerdict = appLicensingVerdict;
        this.requestPackageName = requestPackageName;
        this.nonce = nonce;
        this.error = error;
    }

    public AppIntegrityEntity() {
    }

    @NonNull
    public String getSno() {
        return sno;
    }

    public void setSno(@NonNull String sno) {
        this.sno = sno;
    }

    public String getAppIntegrity() {
        return appIntegrity;
    }

    public void setAppIntegrity(String appIntegrity) {
        this.appIntegrity = appIntegrity;
    }

    public String getDeviceIntegrity() {
        return deviceIntegrity;
    }

    public void setDeviceIntegrity(String deviceIntegrity) {
        this.deviceIntegrity = deviceIntegrity;
    }

    public String getAppLicensingVerdict() {
        return appLicensingVerdict;
    }

    public void setAppLicensingVerdict(String appLicensingVerdict) {
        this.appLicensingVerdict = appLicensingVerdict;
    }

    public String getRequestPackageName() {
        return requestPackageName;
    }

    public void setRequestPackageName(String requestPackageName) {
        this.requestPackageName = requestPackageName;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
