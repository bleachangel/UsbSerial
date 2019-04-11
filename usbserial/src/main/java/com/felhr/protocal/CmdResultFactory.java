package com.felhr.protocal;

public class CmdResultFactory {
    public static final String CMD_SETUP_TAG    = ":STP";
    public static final String CMD_TEST_TAG     = ":TST";
    public static final String CMD_RESET_TAG    = ":RST";
    public static final String CMD_I2C_READ_TAG   = ":I2R";
    public static final String CMD_I2C_WRITE_TAG   = ":I2W";
    public static final String CMD_AUTO_REPORT_TAG   = ":ATR";
    public static final String CMD_GPIO_READ_TAG   = ":IOR";
    public static final String CMD_GPIO_WRITE_TAG   = ":IOW";
    public static final String CMD_SET_VOL_TAG   = ":SVL";
    public static final String CMD_GET_VOL_TAG   = ":GVL";
    public static final String CMD_SET_LCD_BRIGHT_TAG   = ":SLB";
    public static final String CMD_GET_LCD_BRIGHT_TAG   = ":GLB";
    public static final String CMD_OPEN_CAMERA_TAG   = ":OPC";
    public static final String CMD_CLOSE_CAMERA_TAG   = ":CLC";
    public static final String CMD_OPEN_FLASH_LIGHT_TAG   = ":OFL";
    public static final String CMD_CLOSE_FLASH_LIGHT_TAG   = ":CFL";
    public static final String CMD_I2C_CONFIG_TAG   = ":I2C";
    public static final String CMD_SET_MIC_VOL_TAG   = ":SMV";
    public static final String CMD_GET_MIC_VOL_TAG   = ":GMV";
    public static final String CMD_END_TAG   = ":END";

    public static final byte CMD_START_TAG = ':';
    public static final int CMD_TAG_LEN = 4;

    public static final int CMD_SETUP_VALUE    = 1;
    public static final int CMD_TEST_VALUE     = 2;
    public static final int CMD_RESET_VALUE    = 3;
    public static final int CMD_I2C_READ_VALUE   = 4;
    public static final int CMD_I2C_WRITE_VALUE   = 5;
    public static final int CMD_GPIO_READ_VALUE   = 6;
    public static final int CMD_GPIO_WRITE_VALUE   = 7;
    public static final int CMD_SET_VOL_VALUE   = 8;
    public static final int CMD_GET_VOL_VALUE   = 9;
    public static final int CMD_SET_LCD_BRIGHT_VALUE   = 10;
    public static final int CMD_GET_LCD_BRIGHT_VALUE   = 11;
    public static final int CMD_OPEN_CAMERA_VALUE   = 12;
    public static final int CMD_CLOSE_CAMERA_VALUE   = 13;
    public static final int CMD_OPEN_FLASH_LIGHT_VALUE   = 14;
    public static final int CMD_CLOSE_FLASH_LIGHT_VALUE   = 15;
    public static final int CMD_I2C_CONFIG_VALUE   = 16;
    public static final int CMD_SET_MIC_VOL_VALUE   = 17;
    public static final int CMD_GET_MIC_VOL_VALUE   = 18;
    public static final int CMD_AUTO_REPORT_VALUE   = 19;
    public static final int CMD_END_VALUE   = 20;
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

        //byte[] data = convert(para);
        if(para.length <= 0){
            return null;
        }

        if(cmdType.equalsIgnoreCase(CMD_SETUP_TAG)){
            return new SetupCmdResult(CMD_SETUP_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_TEST_TAG)){
        } else if(cmdType.equalsIgnoreCase(CMD_RESET_TAG)){
            return new ResetCmdResult(CMD_RESET_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_I2C_READ_TAG)){
            return  new I2CReadCmdResult(CMD_I2C_READ_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_AUTO_REPORT_TAG)){
            return  new ATRCmdResult(CMD_AUTO_REPORT_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_I2C_WRITE_TAG)){
            return  new I2CWriteCmdResult(CMD_I2C_WRITE_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_GPIO_READ_TAG)){
            return new IOReadCmdResult(CMD_GPIO_READ_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_GPIO_WRITE_TAG)){
            return  new IOWriteCmdResult(CMD_GPIO_WRITE_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_SET_VOL_TAG)){
            return  new SVLCmdResult(CMD_SET_VOL_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_GET_VOL_TAG)){
            return  new GVLCmdResult(CMD_GET_VOL_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_SET_LCD_BRIGHT_TAG)){
            return  new SLBCmdResult(CMD_SET_LCD_BRIGHT_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_GET_LCD_BRIGHT_TAG)){
            return  new GLBCmdResult(CMD_GET_LCD_BRIGHT_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_OPEN_CAMERA_TAG)){
            return  new OPCCmdResult(CMD_OPEN_CAMERA_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_CLOSE_CAMERA_TAG)){
            return  new CLCCmdResult(CMD_CLOSE_CAMERA_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_OPEN_FLASH_LIGHT_TAG)){
            return  new OFLCmdResult(CMD_OPEN_FLASH_LIGHT_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_CLOSE_FLASH_LIGHT_TAG)){
            return  new CFLCmdResult(CMD_CLOSE_FLASH_LIGHT_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_I2C_CONFIG_TAG)){
            return  new I2CConfigCmdResult(CMD_I2C_CONFIG_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_SET_MIC_VOL_TAG)){
            return  new SVLCmdResult(CMD_SET_MIC_VOL_VALUE, para);
        } else if(cmdType.equalsIgnoreCase(CMD_GET_MIC_VOL_TAG)){
            return  new GVLCmdResult(CMD_GET_MIC_VOL_VALUE, para);
        }

        return null;
    }

}
