package org.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AddTaskCommandTest {
    @Test
    public void addTaskCommandTest(){
        new KeyboardConfiguration();
        addTaskCommandTest("10.10.2021 9:00 - 10:00", true);
        addTaskCommandTest("1.1.2021 9:0 - 10:0", true);
        addTaskCommandTest("29.02.2024 9:00 - 10:00", true);
        addTaskCommandTest("29.02.2021 9:00 - 10:00", false);
        addTaskCommandTest("10.10.2020 9:00 - 10:00", false);

        addTaskCommandTest("10.10.2021 10:00 - 9:00", false);
        addTaskCommandTest("10.10.2021 -1:00 - 10:00", false);
        addTaskCommandTest("10.10.2021 9:70 - 10:00", false);
        addTaskCommandTest("10.10.2021 9:00 - 25:00", false);
        addTaskCommandTest("10.10.2021 9:00", false);

        addTaskCommandTest("10.10.2021 9:00-10:00", false);
        addTaskCommandTest("10.10.2021 9:00- 10:00", false);
        addTaskCommandTest("10.10.2021 9:00 : 10:00", false);
        addTaskCommandTest("10.2021 9:00 - 10:00", false);
        addTaskCommandTest("-10.10.2021 9:00 - 10:00", false);

        addTaskCommandTest("10.20.2021 9:00 - 10:00", false);
        addTaskCommandTest("10.20.2021 9:00 - 10:00", false);
    }

    @Test
    public void addRepetitiveTaskCommandTest(){
        new KeyboardConfiguration();
        addRepetitiveTaskCommandTest("9:00 - 10:00", true);

        addRepetitiveTaskCommandTest("9:70 - 10:00", false);
        addRepetitiveTaskCommandTest("-1:00 - 10:00", false);
        addRepetitiveTaskCommandTest("11:00 - 10:00", false);
        addRepetitiveTaskCommandTest("9:00 : 10:00", false);
        addRepetitiveTaskCommandTest("9:00-10:00", false);
    }

    public void addTaskCommandTest(String dateTime, Boolean correctFormat){
        var firstBotRequestText = "Choose the date";
        var secondBotRequestText = "Write time interval of your task in format: 9:00 - 10:00";

        var userAnswer = makeUserAnswer(dateTime, dateTime.split(" ")[0]);

        checkBotRequests(userAnswer, dateTime, new AddTask(),
                firstBotRequestText, secondBotRequestText, correctFormat);
    }

    public void addRepetitiveTaskCommandTest(String timeInterval, Boolean correctFormat){
        var botRequestText = "Write time interval of your task in format: 9:00 - 10:00";
        var botRequestErrorMessage = "Write time interval of your task in format: 9:00 - 10:00";

        var userAnswer = makeUserAnswer(timeInterval, "OK");

        checkBotRequests(userAnswer, timeInterval, new AddRepetitiveTask(),
                botRequestText, botRequestErrorMessage, correctFormat);
    }

    private Update makeUserAnswer(String botRequestText, String callbackData){
        var currentChat = new Chat();
        currentChat.setId(1L);

        var messageForUserAnswer = new Message();
        messageForUserAnswer.setText(botRequestText);
        messageForUserAnswer.setChat(currentChat);

        var userAnswer = new Update();
        userAnswer.setMessage(messageForUserAnswer);
        userAnswer.setCallbackQuery(new CallbackQuery(
                null, null, null, null, callbackData, null, null));
        userAnswer.getCallbackQuery().setMessage(userAnswer.getMessage());

        return userAnswer;
    }

    private void checkBotRequests(Update userAnswer, String datetime, BotCommand addTaskCommand,
                                  String firstRequestText, String firstRequestErrorMessage, Boolean correctFormat){
        var botRequestAboutTaskInfo = addTaskCommand.exec(userAnswer);

        assertEquals(firstRequestText,
                ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

        botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);

        assert(!(botRequestAboutTaskInfo instanceof StandardBotRequest));
        if (!correctFormat)
            assertEquals(firstRequestErrorMessage,
                    ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());
        else
            checkBotRequestsFromNameToType(datetime, botRequestAboutTaskInfo, userAnswer, addTaskCommand);
    }

    private void checkBotRequestsFromNameToType(String datetime, BotRequest botRequestAboutTaskInfo,
                                                Update userAnswer, BotCommand addTaskCommand){
        if (!(addTaskCommand instanceof AddRepetitiveTask)){
            assertEquals("Write time interval of your task in format: 9:00 - 10:00",
                    ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

            var time = datetime.split(" ");
            assert time.length == 4;
            var timeInterval = time[1] + " " + time[2] + " " + time[3];
            userAnswer.getMessage().setText(timeInterval);
            botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);
        }
        assertEquals("Write name for your task", ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

        userAnswer.getMessage().setText("name");
        botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);
        assertEquals("Write description for your task", ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

        userAnswer.getMessage().setText("description");
        botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);
        assertEquals("Write 1 if your task is overlapping, 2 if nonOverlapping and 3 if important",
                ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

        userAnswer.getMessage().setText("wrong answer");
        botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);
        assert(!(botRequestAboutTaskInfo instanceof StandardBotRequest));
        assertEquals("Error: Wrong value for task type. Please try again and write 1 if your task is overlapping, 2 if nonOverlapping and 3 if important",
                ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());

        userAnswer.getMessage().setText("3");
        botRequestAboutTaskInfo = botRequestAboutTaskInfo.handle(userAnswer, null);
        assert(botRequestAboutTaskInfo instanceof StandardBotRequest);
        assertEquals("Task was added", ((SendMessage)botRequestAboutTaskInfo.getRequestMessage()).getText());
    }
}
