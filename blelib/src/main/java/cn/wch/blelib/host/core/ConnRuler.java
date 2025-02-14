package cn.wch.blelib.host.core;


import androidx.annotation.NonNull;

public final class ConnRuler {
    private final String MAC;
    private final long connectTimeout;

    private final int readNullRetryCount;

    private final long readTimeout;
    private final long writeTimeout;

    private ConnRuler(Builder builder) {
        this.MAC = builder.MAC;
        this.connectTimeout = builder.connectTimeout;
        this.readNullRetryCount=builder.readNullRetryCount+1;
        this.readTimeout=builder.readTimeout;
        this.writeTimeout=builder.writeTimeout;
    }

    public String getMAC() {
        return MAC;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public int getReadNullRetryCount() {
        return readNullRetryCount;
    }

    public static class Builder {
        private String MAC=null;
        private long connectTimeout=15000;
        private int readNullRetryCount=10;
        public long readTimeout=1000;
        public long writeTimeout=1000;

        public Builder(String MAC) {
            this.MAC = MAC;
        }


        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(long readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder writeTimeout(long writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public ConnRuler build(){
            return new ConnRuler(this);
        }
    }
}
