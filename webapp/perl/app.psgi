use FindBin;
use lib "$FindBin::Bin/local/lib/perl5";
use lib "$FindBin::Bin/lib";
use File::Basename;
use Plack::Builder;
use Isucon5f::Web;

my $root_dir = File::Basename::dirname(__FILE__);

my $app = Isucon5f::Web->psgi($root_dir);
builder {
    enable 'ReverseProxy';
    enable 'Static',
        path => qr!^/(?:(?:css|fonts|js)/|favicon\.ico$)!,
        root => File::Basename::dirname($root_dir) . '/static';
    enable 'Session::Cookie',
        session_key => "airisu_session",
        secret => $ENV{ISUCON5_SESSION_SECRET} || 'tonymoris',
    ;
    $app;
};
