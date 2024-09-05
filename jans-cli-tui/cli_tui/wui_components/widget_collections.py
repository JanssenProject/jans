from utils.multi_lang import _
from utils.utils import DialogUtils, common_data
from utils.static import cli_style
from wui_components.jans_spinner import Spinner
from wui_components.jans_drop_down import DropDownWidget


def get_ldap_config_widgets(data, widget_style=cli_style.white_bg_widget):


    bind_dn_default = data.get('bindDN','')
    bind_password_default = data.get('bindPassword','')

    bind_dn_widget = common_data.app.getTitledText(
                title=_("Bind DN"),
                name='bindDN',
                value=data.get('bindDN',''),
                style=cli_style.edit_text_required,
                widget_style=widget_style
            )

    bind_password_widget = common_data.app.getTitledText(
                title=_("Bind Password"),
                name='bindPassword',
                value=data.get('bindPassword') or '',
                style=cli_style.edit_text_required,
                jans_help=_("Bind password for LDAP"),
                widget_style=widget_style
            )

    def make_anonymous_bind(cb=None):
        if getattr(cb, 'checked', False):
            bind_dn_widget.me.text = ''
            bind_password_widget.me.text = ''
            bind_dn_widget.me.read_only = True
            bind_password_widget.me.read_only = True
            bind_dn_widget.me.style = cli_style.edit_text
            bind_password_widget.me.style = cli_style.edit_text
            bind_dn_widget.label_widget.style = cli_style.edit_text
            bind_password_widget.label_widget.style = cli_style.edit_text
        else:
            bind_dn_widget.me.text = bind_dn_default
            bind_dn_widget.me.read_only = False
            bind_password_widget.me.text = bind_password_default
            bind_password_widget.me.read_only = False
            bind_dn_widget.me.style = cli_style.edit_text_required
            bind_password_widget.me.style = cli_style.edit_text_required
            bind_dn_widget.label_widget.style = cli_style.edit_text_required
            bind_password_widget.label_widget.style = cli_style.edit_text_required

    ldap_config_widgets = [

            common_data.app.getTitledText(
                title=_("Name"),
                name='configId',
                value=data.get('configId',''),
                style=cli_style.edit_text_required,
                widget_style=widget_style
            ),

            bind_dn_widget,
            bind_password_widget,

            common_data.app.getTitledWidget(
                title=_("Max Connections"),
                name='maxConnections',
                widget=Spinner(value=data.get('maxConnections', 2)),
                style=cli_style.edit_text_required,
            ),

            common_data.app.getTitledText(
                title=_("Server:Port"),
                name='servers',
                value=' '.join(data.get('servers', [])),
                style=cli_style.edit_text_required,
                jans_help=_("Space seperated server:port"),
                jans_list_type=True,
                widget_style=widget_style
            ),

            common_data.app.getTitledText(
                title=_("Base DNs"),
                name='baseDNs',
                value=' '.join(data.get('baseDNs',[])),
                style=cli_style.edit_text_required,
                jans_help=_("Space seperated base dns"),
                jans_list_type=True,
                widget_style=widget_style
            ),

            common_data.app.getTitledText(
                title=_("Primary Key"),
                name='primaryKey',
                value=data.get('primaryKey') or '',
                style=cli_style.edit_text,
                jans_help=_("Primary key"),
                widget_style=widget_style
            ),

            common_data.app.getTitledText(
                title=_("Local Primary Key"),
                name='localPrimaryKey',
                value=data.get('localPrimaryKey') or '',
                style=cli_style.edit_text,
                jans_help=_("Local Primary Key"),
                widget_style=widget_style
            ),

            common_data.app.getTitledCheckBox(
                title=_("Use Anonymous Bind"), 
                name='useAnonymousBind', 
                checked=data.get('useAnonymousBind', False),
                style=cli_style.check_box,
                widget_style=widget_style,
                on_selection_changed=make_anonymous_bind
            ),

            common_data.app.getTitledCheckBox(
                title=_("Use SSL"), 
                name='useSSL', 
                checked=data.get('useSSL', False),
                style=cli_style.check_box,
                widget_style=widget_style
            ),

            common_data.app.getTitledCheckBox(
                title=_("Enable"), 
                name='enabled', 
                checked=data.get('enabled', False),
                style=cli_style.check_box,
                widget_style=widget_style
            ),
        ]

    return ldap_config_widgets

def get_data_for_ldap_widgets(container):
    du = DialogUtils()
    data = du.make_data_from_dialog(tabs={'_': container})
    return data

def get_logging_level_widget(level='INFO', name='loggingLevel', style=cli_style.edit_text):
    return common_data.app.getTitledWidget(
                    _("Logging Level"),
                    name=name,
                    widget=DropDownWidget(
                        values=[('TRACE', 'TRACE'), ('DEBUG', 'DEBUG'), ('INFO', 'INFO'), ('WARN', 'WARN'),('ERROR', 'ERROR'),('FATAL', 'FATAL'),('OFF', 'OFF')],
                        value=level,
                        select_one_option=False
                        ),
                    jans_help=_("Chose logging level for service"),
                    style=style
                )
