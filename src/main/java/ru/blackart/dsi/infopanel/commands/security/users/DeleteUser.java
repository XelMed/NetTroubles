package ru.blackart.dsi.infopanel.commands.security.users;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import ru.blackart.dsi.infopanel.beans.User;
import ru.blackart.dsi.infopanel.commands.AbstractCommand;
import ru.blackart.dsi.infopanel.SessionFactorySingle;

import java.util.ArrayList;

public class DeleteUser extends AbstractCommand {
    @Override
    public String execute() throws Exception {

        String user_id = this.getRequest().getParameter("id");

        Session session = SessionFactorySingle.getSessionFactory().openSession();
        session.beginTransaction();

        Criteria crt_users = session.createCriteria(User.class);
        crt_users.add(Restrictions.eq("id", Integer.valueOf(user_id)));
        User user = (User)crt_users.list().get(0);

        session.delete(user);

        session.getTransaction().commit();
        session.flush();
        session.close();

        session = SessionFactorySingle.getSessionFactory().openSession();
        session.beginTransaction();

        Criteria crt_5 = session.createCriteria(User.class);
        ArrayList<User> users = new ArrayList<User>(crt_5.list());
        this.getConfig().getServletContext().setAttribute("users", users);

        session.getTransaction().commit();
        session.flush();
        session.close();

        return null;
    }
}
