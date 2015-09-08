package jp.gecko655.bot.tomorinao;


import java.text.DateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jp.gecko655.bot.AbstractCron;
import jp.gecko655.bot.DBConnection;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;

public class TomoriNaoReply extends AbstractCron {
    
    static final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
    private static final Pattern whoPattern = Pattern.compile("( 誰$| だれ$|誰[^だで]|だれ[^だで]|誰だ[^と]?|だれだ[^と]?| 違う| ちがう)");

    public TomoriNaoReply() {
        format.setTimeZone(TimeZone.getDefault());
    }

    @Override
    protected void twitterCron() {
        try {
            Status lastStatus = DBConnection.getLastStatus();
            List<Status> replies = twitter.getMentionsTimeline((new Paging()).count(20));
            if(replies.isEmpty()){
                logger.log(Level.INFO, "Not yet replied. Stop.");
                return;
            }
            DBConnection.setLastStatus(replies.get(0));
            if(lastStatus == null){
                logger.log(Level.INFO,"memcache saved. Stop. "+replies.get(0).getUser().getName()+"'s tweet at "+format.format(replies.get(0).getCreatedAt()));
                return;
            }
            List<Status> validReplies = replies.stream().filter(reply -> isValid(reply, lastStatus)).collect(Collectors.toList());
            if(validReplies.isEmpty()){
                logger.log(Level.FINE, "No valid replies. Stop.");
                return;
            }
            
            for(Status reply : validReplies){
                Relationship relation = twitter.friendsFollowers().showFriendship(twitter.getId(), reply.getUser().getId());
                if(!relation.isSourceFollowingTarget()){
                    twitter.createFriendship(reply.getUser().getId());
                }

                if(whoPattern.matcher(reply.getText()).find()){
                    // put latest image URL to black-list
                    who(reply);    
                }else{
                    //auto reply (when bot follows the replier)
                    StatusUpdate update= new StatusUpdate("@"+reply.getUser().getScreenName()+" ");
                    update.setInReplyToStatusId(reply.getId());
                    String query = query();
                    updateStatusWithMedia(update, query, 100);
                }
            }
        } catch (TwitterException e) {
            logger.log(Level.WARNING,e.toString());
            e.printStackTrace();
        }
    }
    
    private String query(){
        int rand = (int) Math.random()*10;
        if(rand<6){//60%
            return "友利奈緒 かわいい";
        }else if(rand<8){//20%
            return "西森柚咲 かわいい";
        }else{//20%
            return "乙坂歩未 かわいい";
        }
    }

    private boolean isValid(Status reply, Status lastStatus) {
        if(lastStatus==null) return false;
        return reply.getCreatedAt().after(lastStatus.getCreatedAt());
    }

    private void who(Status reply) {
        //Store the url to the black list.
        DBConnection.storeImageUrlToBlackList(reply.getInReplyToStatusId(),reply.getUser().getScreenName());

        try{
            //Delete the reported tweet.
            twitter.destroyStatus(reply.getInReplyToStatusId());
            
            //Apologize to the report user.
            StatusUpdate update= new StatusUpdate("@"+reply.getUser().getScreenName()+" 間違えちゃった。ごめんね！");
            update.setInReplyToStatusId(reply.getId());
            twitter.updateStatus(update);
        }catch(TwitterException e){
            e.printStackTrace();
        }
    }
}
