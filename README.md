## Deploy Instructions

### Setup environment

1. Install Java 11 JDK (And add an alias to your `.bashrc`)

```shell
alias java11='/usr/lib/jvm/jre-11/bin/java'
```   

2. Create deployment directory `/app/depl/clock-api`
3. Create `/app/depl/clock-api/launch.sh` (Don't forget to `chmod +x`)

```shell
/usr/lib/jvm/jre-11/bin/java -Xms512M -Xmx1024M -Dserver.port=41566 -jar clock_api.jar
```

4. Run a test `./launch.sh` to see if it runs
5. Create `/etc/systemd/system/clock-api.service`

```ini
[Unit]
Description=iOS Alarm Clock API Server

[Service]
WorkingDirectory=/app/depl/clock-api/
ExecStart=/bin/bash launch.sh
User=jvmapps
Type=simple
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

6. If you haven't set up the execution user yet:

```shell
groupadd -r appmgr
sudo useradd -r -s /bin/false -g appmgr jvmapps
id jvmapps
chown -R jvmapps:appmgr /app/depl/clock-api
```

7. Run `deploy.sh` in your local machine (Don't forget to change `HOST`)
8. Add aliases to your `.bashrc`

```shell
alias clock-restart='sctl restart clock-api'
alias clock-log-all='jctl -u clock-api --output cat'
alias clock-log='clock-log-all -f'
```

9. Add Nginx mapping in `/etc/nginx/nginx.conf` (Change the host names to your host name)

```nginx.conf
# Clock API HTTP
server
{
    listen 80;
    listen [::]:80;
    server_name alarm-clock-api.hydev.org;

    location ^~ /
    {
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        proxy_pass http://alarm-clock-api.hydev.org:41566/;
        proxy_redirect off;
    }
}
```

10. Reload Nginx `systemctl restart nginx`
11. Get HTTPS `certbot`
12. Clean up the indentation in `/etc/nginx/nginx.conf` because `certbot` always makes a mess ;-;  
    The final result should look something like:

```nginx.conf
# Clock API HTTP Redirect to HTTPS
server
{
    listen 80;
    listen [::]:80;
    server_name alarm-clock-api.hydev.org;
    return 301 https://$host$request_uri;
}

# Clock API HTTPS
server
{
    server_name alarm-clock-api.hydev.org;

    location ^~ /
    {
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        proxy_pass http://alarm-clock-api.hydev.org:41566/;
        proxy_redirect off;
    }

    listen [::]:443 ssl; # managed by Certbot
    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/alarm-clock-api.hydev.org/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/alarm-clock-api.hydev.org/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
}
```

13. Enable auto start `systemctl enable clock-api`

## Node

**GET** `/register`

|    Name    |   Type   |    In    |                         Description                         |
| :--------: | :------: | :------: | :---------------------------------------------------------: |
| `username` | `string` | `header` | User's name, should match the regex `/^[a-z0-9_-]{3,16}$/`. |
| `password` | `string` | `header` |              Only password's md5 will be save.              |

**Response**

| Scenario |     Http Status      |     Type     |          Value           |
| :------: | :------------------: | :----------: | :----------------------: |
| Success  |       `200 OK`       |   `string`   |       User's uuid        |
| Failure  | `406 NOT ACCEPTABLE` | `json array` | JSON Array of Error Code |

## Error Code

|  Code   |           Description           |
| :-----: | :-----------------------------: |
| `A0101` |       `username` is null.       |
| `A0102` |       `password` is null        |
| `A0111` | `username` not match the regex. |
| `A0112` | `password` not match the regex. |

