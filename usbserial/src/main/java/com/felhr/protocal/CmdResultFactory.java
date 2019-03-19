package com.felhr.protocal;

public class CmdResultFactory {
    public static final String CMD_SETUP_TAG    = ":STP";
    public static final String CMD_GET_TAG      = ":GET";
    public static final String CMD_OPEN_TAG     = ":OPN";
    public static final String CMD_CLOSE_TAG    = ":CLS";
    public static final String CMD_ENABLE_TAG   = ":ENA";
    public static final String CMD_TEST_TAG     = ":TST";
    public static final String CMD_RESET_TAG    = ":RST";
    public static final String CMD_I2C_READ_TAG   = ":I2R";
    public static final String CMD_I2C_WRITE_TAG   = ":I2W";
    public static final String CMD_GPIO_READ_TAG   = ":IOR";
    public static final String CMD_GPIO_WRITE_TAG   = ":IOW";
    public static final String CMD_END_TAG   = ":END";

    public static final byte CMD_START_TAG = ':';
    public static final int CMD_TAG_LEN = 4;

    public static final int CMD_SETUP_VALUE    = 1;
    public static final int CMD_GET_VALUE      = 2;
    public static final int CMD_OPEN_VALUE     = 3;
    public static final int CMD_CLOSE_VALUE    = 4;
    public static final int CMD_ENABLE_VALUE   = 5;
    public static final int CMD_TEST_VALUE     = 6;
    public static final int CMD_RESET_VALUE    = 7;
    public static final int CMD_I2C_READ_VALUE   = 8;
    public static final int CMD_I2C_WRITE_VALUE   = 9;
    public static final int CMD_GPIO_READ_VALUE   = 10;
    public static final int CMD_GPIO_WRITE_VALUE   = 11;
    public static final int CMD_END_VALUE   = 12;
    public static final int CMD_INVALID_VALUE    = -1;

    public static byte[] convert(byte[] chars){
        int strLen =chars.length;
        int len = strLen/2;
        byte[] result = new byte[len];
        if(len > 0){
            for(int i = 0; i < len; i++){
                if(chars[i*2] >= '0' && chars[i*2] <= '9'){
                    result[i] = (byte) (((chars[i*2] - '0')&0xF)<<4);
                } else if(chars[i*2] >= 'A' && chars[i*2] <= 'F'){
                    result[i] = (byte) (((chars[i*2] - 'A' + 10)&0xF)<<4);
                } else {
                    //error code
                    len = -1;
                    break;
                }

                if(chars[i*2 + 1] >= '0' && chars[i*2 + 1] <= '9'){
                    result[i] |= (byte) ((chars[i*2 + 1] - '0')&0xF);
                } else if(chars[i*2 + 1] >= 'A' && chars[i*2 + 1] <= 'F'){
                    result[i] |= (byte) ((chars[i*2 + 1] - 'A' + 10)&0xF);
                } else {
                    //error code
                    len = -1;
                    break;
                }
            }
        }
        if(len == -1){
            return null;
        }

        return result;
    }

    //使用 getShape 方法获取形状类型的对象
    static public ProtocalCmd getCmdResult(String cmdType, byte[] para){
        if(cmdType == null){
            return null;
        }

        byte[] data = convert(para);

        if(cmdType.equalsIgnoreCase(CMD_SETUP_TAG)){
            return new SetupCmdResult(CMD_SETUP_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_GET_TAG)){
            return  new GetCmdResult(CMD_GET_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_TEST_TAG)){
        } else if(cmdType.equalsIgnoreCase(CMD_RESET_TAG)){
            return new ResetCmdResult(CMD_RESET_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_I2C_READ_TAG)){
            return  new I2CReadCmdResult(CMD_I2C_READ_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_I2C_WRITE_TAG)){
            return  new I2CWriteCmdResult(CMD_I2C_WRITE_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_GPIO_READ_TAG)){
            return new IOReadCmdResult(CMD_GPIO_READ_VALUE, data);
        } else if(cmdType.equalsIgnoreCase(CMD_GPIO_WRITE_TAG)){
            return  new IOWriteCmdResult(CMD_GPIO_WRITE_VALUE, data);
        }

        return null;
    }

}
