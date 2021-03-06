package ru.blackart.dsi.infopanel.controllers;

import com.google.gson.Gson;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.blackart.dsi.infopanel.access.AccessTab;
import ru.blackart.dsi.infopanel.access.menu.*;
import ru.blackart.dsi.infopanel.beans.Group;
import ru.blackart.dsi.infopanel.commands.FactoryCommandCommand;
import ru.blackart.dsi.infopanel.commands.Command;
import ru.blackart.dsi.infopanel.SessionFactorySingle;
import ru.blackart.dsi.infopanel.access.AccessItemMenu;
import ru.blackart.dsi.infopanel.access.AccessMenuForGroup;
import ru.blackart.dsi.infopanel.access.AccessUserObject;
import ru.blackart.dsi.infopanel.beans.*;
import ru.blackart.dsi.infopanel.commands.access.Login;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;

import org.hibernate.Session;
import org.hibernate.Criteria;
import ru.blackart.dsi.infopanel.services.DeviceManager;
import ru.blackart.dsi.infopanel.utils.TroubleListsManager;

public class HTTPServletController extends HttpServlet {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private FactoryCommandCommand factory;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        //создаем экземпляр синглтона, устанавливаем в него ссылку на конфиг HTTPSC, чтобы записывать в сервлет
        //контекст инфу из SNMPc контроллера.
        TroubleListsManager.getInstance().setServletConfig(config);

        String path = config.getInitParameter("pathToDataFile");
        InputStream is = config.getServletContext().getResourceAsStream(path);
        Properties path_to_data_file = new Properties();
        try {
            path_to_data_file.load(is);
        } catch (IOException e) {

        }
        config.getServletContext().setAttribute("pathToDataFile", path_to_data_file);

        //пути к конфигурационным файлам
        Properties paths = (Properties) config.getServletContext().getAttribute("pathToDataFile");
        // подгружаем настройки логера
        File config_log4j = new File(config.getInitParameter("pathToCatalina") + paths.getProperty("pathToApplication") + config.getInitParameter("root") + paths.getProperty("pathToLog4jConfig"));
//        File config_log4j = new File("\\classes\\log4j.properties");
        PropertyConfigurator.configure(config_log4j.getPath());
        //инициализируем Фабрику Комманд
        this.factory = FactoryCommandCommand.getInstance(this.getServletConfig());

        //забираем из базы занчения
        Session session = SessionFactorySingle.getSessionFactory().openSession();

        //Services
        Criteria crt_0 = session.createCriteria(Service.class);
        ArrayList<Service> services = new ArrayList<Service>(crt_0.list());
        config.getServletContext().setAttribute("services", services);

        Criteria crt_2 = session.createCriteria(TypeDeviceFilter.class);
        ArrayList<TypeDeviceFilter> typeDeviceFilters = new ArrayList<TypeDeviceFilter>(crt_2.list());
        config.getServletContext().setAttribute("typeDeviceFilters", typeDeviceFilters);

        Criteria crt_4 = session.createCriteria(Tab.class);
        ArrayList<Tab> tabs = new ArrayList<Tab>(crt_4.list());
        config.getServletContext().setAttribute("tabs", tabs);

        List<AccessItemMenu> generalMenu = AccessUserObject.generateMenuTabs(tabs);
        config.getServletContext().setAttribute("generalMenu", generalMenu);

        Criteria crt_3 = session.createCriteria(Group.class);
        ArrayList<Group> groups = new ArrayList<Group>(crt_3.list());
        ArrayList<AccessMenuForGroup> tabs_of_groups = new ArrayList<AccessMenuForGroup>();
        for (Group g : groups) {
            g.setTabs(new ArrayList<Tab>(g.getTabs()));
            tabs_of_groups.add(new AccessMenuForGroup(g,tabs));
        }

        config.getServletContext().setAttribute("tabs_of_groups", tabs_of_groups);
        config.getServletContext().setAttribute("groups", groups);


        //todo генерация json конфигурации меню, убрать после адаптации для ДСИ
        Gson gson = new Gson();

        for (int i = 0; i < tabs_of_groups.size(); i++) {
            AccessMenuForGroup accessMenuForGroup = tabs_of_groups.get(i);
            ArrayList menuItems = (ArrayList) accessMenuForGroup.getItemMenu();

            Menu menu = new Menu();
            ArrayList<ru.blackart.dsi.infopanel.access.menu.Group> groups_ = new ArrayList<ru.blackart.dsi.infopanel.access.menu.Group>();

            for (int j = 0; j < menuItems.size(); j++) {
                AccessItemMenu accessItemMenu = (AccessItemMenu) menuItems.get(j);
                AccessTab accessTab = accessItemMenu.getTab();
                if (accessTab.isPolicy()) {
                    ru.blackart.dsi.infopanel.access.menu.Group group = new ru.blackart.dsi.infopanel.access.menu.Group();
                    group.setId(accessTab.getTab().getMenu_group());
                    group.setName(accessTab.getTab().getCaption());
                    group.setUrl(accessItemMenu.getChildrens().size() > 0 ? null : accessTab.getTab().getFile_name());
                    if (accessItemMenu.getChildrens().size() > 0) {
                        List<Item> items = new ArrayList<Item>();
                        for (int k = 0; k < accessItemMenu.getChildrens().size(); k++) {
                            AccessTab children = accessItemMenu.getChildrens().get(k);
                            if (children.isPolicy()) {
                                Item item = new Item();
                                Tab tab_children = children.getTab();
                                item.setName(tab_children.getCaption());
                                item.setPosition(tab_children.getGroup_position());
                                item.setUrl(tab_children.getFile_name());
                                items.add(item);
                            }
                        }
                        group.setItems(items);
                    }
                    groups_.add(group);
                }
            }
            menu.setGroups(groups_);
            Group group_s = accessMenuForGroup.getGroup();
            group_s.setMenuConfig(gson.toJson(menu));
            session.getTransaction().begin();
            session.save(group_s);
            session.getTransaction().commit();
        }

        //Users
        Criteria crt_5 = session.createCriteria(Users.class);
        ArrayList<Users> users = new ArrayList<Users>(crt_5.list());
        config.getServletContext().setAttribute("users", users);

        //Hostgroups
        Criteria crt_6 = session.createCriteria(Hostgroup.class);
        ArrayList<Hostgroup> hostgroups = new ArrayList<Hostgroup>(crt_6.list());
        config.getServletContext().setAttribute("hostgroups", hostgroups);

        //Hoststatus
        Criteria crt_host_status = session.createCriteria(Hoststatus.class);
        ArrayList<Hoststatus> hoststatuses = new ArrayList<Hoststatus>(crt_host_status.list());
        config.getServletContext().setAttribute("hoststatuses", hoststatuses);

        //Region
        Criteria crt_region = session.createCriteria(Region.class);
        ArrayList<Region> regions = new ArrayList<Region>(crt_region.list());
        config.getServletContext().setAttribute("regions", regions);

        //Devcapsules
        Criteria crt_8 = session.createCriteria(Devcapsule.class);
        ArrayList<Devcapsule> devcapsules = new ArrayList<Devcapsule>(crt_8.list());
        config.getServletContext().setAttribute("devcapsules", devcapsules);

        //TroubleLists
        Criteria crt_10 = session.createCriteria(TroubleList.class);
        ArrayList<TroubleList> troubleLists = new ArrayList<TroubleList>(crt_10.list());
        config.getServletContext().setAttribute("troubleLists", troubleLists);

        session.flush();
        session.close();

        //Devices
        DeviceManager deviceManager = DeviceManager.getInstance();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setCharacterEncoding("utf-8");

        //Проверка начальных параметров сессии пользователя
//        log.info("HTTP Session - " + (request.getSession(true) == null ? "null" : "not null"));
//        log.info("HTTP Session attribute LOGIN - " + (request.getSession(true).getAttribute("login") == null ? "null" : request.getSession(true).getAttribute("login").toString()));

        if (request.getSession(true).getAttribute("login") == null) {
//            log.info("Starting new session for remote host - " + request.getRemoteHost());
            try {
                Login.start(request.getSession(true));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

//        log.info("Remote host " + request.getRemoteHost() + " doing " + request.getMethod() + " request to " + request.getServerName());

        String redirect = null;
        if (request.getParameterMap().containsKey("cmd")) {
            try {
                Command cmd = factory.createClass(request,response,getServletContext(),getServletConfig(),request.getParameter("cmd"));
                redirect = cmd.execute();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            redirect = "index.jsp";
        }

        if (redirect != null) {
            response.sendRedirect(redirect);
        }
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        this.doPost(httpServletRequest,httpServletResponse);
    }
}
