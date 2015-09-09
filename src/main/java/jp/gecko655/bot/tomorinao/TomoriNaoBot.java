package jp.gecko655.bot.tomorinao;

import jp.gecko655.bot.AbstractCron;
import twitter4j.StatusUpdate;

public class TomoriNaoBot extends AbstractCron{

    @Override
    protected void twitterCron() {
        //Twitterに書き出し
        StatusUpdate status = new StatusUpdate(" "); //$NON-NLS-1$
        String query = query();
        updateStatusWithMedia(status, query, 100);
        
    }
    private String query(){
        int rand = (int) (Math.random()*10);
        if(rand<6){//60%
            return "友利奈緒";
        }else if(rand<8){//20%
            return "西森柚咲";
        }else{//20%
            return "乙坂歩未";
        }
        
    }

}
