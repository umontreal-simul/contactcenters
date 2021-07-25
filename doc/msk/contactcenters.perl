&do_require_package ('html');
&do_require_package ('tcode');
&do_require_package ('apidoc');
&do_require_package ("ccsymb");

sub do_env_xmldocenv {
}

&ignore_commands ("javadocenv\nendjavadocenv");

1;
