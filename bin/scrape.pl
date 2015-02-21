#!/usr/bin/env perl
use strict; use warnings;

use FindBin;
use lib "$FindBin::Bin/../lib";

use Data::Dumper;
use Scraper::Monster;
use Text::TermExtract;
use Data::Dumper;
use JSON;
use File::Slurp;

use LWP::UserAgent;
my $ua = LWP::UserAgent->new();

my $scraper = Scraper::Monster->new();
my @categories = $scraper->categories();
my $page_max = 10;

my $cat_map = {};
my $ext = Text::TermExtract->new();

mkdir( "$FindBin::Bin/../resources/keywords/" );

my @allowed_categories = @ARGV;
my %allowed = map { $_ => 1 } @allowed_categories;

CATEGORY:
foreach my $cat ( @categories ) {
    my $first_letter = lc(substr( $cat->{name}, 0, 1 ));
    next unless $allowed{$first_letter};

    my $current_page = 1;
    my $cat_map = {};
    PAGE:
    while ( my @results = $scraper->scrape_page( $cat, $current_page++ ) ){
        foreach my $result ( @results ) {
            if ( my $content = $scraper->advert_content( $result ) ){

                if ( my $json = JSON::to_json( { title => $cat->{name}, body => $content }, { ascii => 1 } ) ){
                    my $resp = $ua->post( 'http://127.0.0.1:47654/seed', Content => $json );
                }

                warn $result->title;
                my @terms = $ext->terms_extract( join( " ", $result->title, $content ) );
                map {
                    $cat_map->{$_}++;
                } @terms;

                # for testing only
                # last PAGE;
            }
        }
        if ( $current_page > $page_max ) {
            last PAGE;
        }
    }
    $ua->post( 'http://127.0.0.1:47654/save' );
    my $json = JSON::to_json( $cat_map, { ascii => 1 } );    
    File::Slurp::write_file( "$FindBin::Bin/../resources/keywords/" . $cat->{name} . ".json", $json );
}

while( my ( $cat, $map ) = each %$cat_map ){
    print "\n\n$cat\n\n";
    my @keys = sort { $map->{$a} <=> $map->{$b} } keys(%$map);
    map { 
        print "\n" . $_ . ": " . $map->{$_};
    } @keys;
}
