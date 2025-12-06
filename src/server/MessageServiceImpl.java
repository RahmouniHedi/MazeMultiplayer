package server;

import CorbaModule.MessageServicePOA;

public class MessageServiceImpl extends MessageServicePOA {
    @Override
    public String getMessageOfTheDay() {
        return "Bienvenue sur le Serveur Maze CORBA ! [Bonus Validé]";
    }
}